package framework.codegen.model.constructor.tree

import framework.api.js.JsClassId
import framework.api.js.JsNullModel
import framework.api.js.JsPrimitiveModel
import framework.api.js.util.isExportable
import framework.api.js.util.jsBooleanClassId
import framework.api.js.util.jsDoubleClassId
import framework.api.js.util.jsNumberClassId
import framework.api.js.util.jsStringClassId
import framework.api.js.util.jsUndefinedClassId
import org.utbot.framework.codegen.domain.ModelId
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgAllocateArray
import org.utbot.framework.codegen.domain.models.CgArrayInitializer
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.tree.CgComponents
import org.utbot.framework.codegen.tree.CgVariableConstructor
import org.utbot.framework.codegen.util.at
import org.utbot.framework.codegen.util.inc
import org.utbot.framework.codegen.util.lessThan
import org.utbot.framework.codegen.util.nullLiteral
import org.utbot.framework.codegen.util.resolve
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class JsCgVariableConstructor(ctx: CgContext) : CgVariableConstructor(ctx) {

    private val nameGenerator = CgComponents.getNameGeneratorBy(context)
    
    override fun getOrCreateVariable(model: UtModel, name: String?): CgValue {
        val modelId: ModelId = context.getIdByModel(model)

        return if (model is UtAssembleModel) valueByModelId.getOrPut(modelId) {
            // TODO SEVERE: May lead to unexpected behavior in case of changes to the original method
            super.getOrCreateVariable(model, name)
        } else valueByModel.getOrPut(model) {
            val baseName = name ?: nameGenerator.nameFrom(model.classId)
            when (model) {
                is JsPrimitiveModel -> CgLiteral(model.classId, model.value)
                is UtArrayModel -> constructArray(model, baseName)
                else -> nullLiteral()
            }
        }
    }

    private val MAX_ARRAY_INITIALIZER_SIZE = 10

    private operator fun UtArrayModel.get(index: Int): UtModel = stores[index] ?: constModel

    private val defaultByPrimitiveType: Map<JsClassId, Any> = mapOf(
        jsBooleanClassId to false,
        jsStringClassId to "default",
        jsUndefinedClassId to 0.0,
        jsNumberClassId to 0.0,
        jsDoubleClassId to Double.POSITIVE_INFINITY
    )

    private infix fun UtModel.isNotJsDefaultValueOf(type: JsClassId): Boolean = !(this isJsDefaultValueOf type)

    private infix fun UtModel.isJsDefaultValueOf(type: JsClassId): Boolean = when (this) {
        is JsNullModel -> type.isExportable
        is JsPrimitiveModel -> value == defaultByPrimitiveType[type]
        else -> false
    }

    private fun CgVariable.setArrayElement(index: Any, value: CgValue) {
        val i = index.resolve()
        this.at(i) `=` value
    }

    private fun basicForLoop(until: Any, body: (i: CgExpression) -> Unit) {
        basicForLoop(start = 0, until, body)
    }

    private fun basicForLoop(start: Any, until: Any, body: (i: CgExpression) -> Unit) {
        forLoop {
            val (i, init) = loopInitialization(jsNumberClassId, "i", start.resolve())
            initialization = init
            condition = i lessThan until.resolve()
            update = i.inc()
            statements = block { body(i) }
        }
    }

    private fun loopInitialization(
        variableType: ClassId,
        baseVariableName: String,
        initializer: Any?
    ): Pair<CgVariable, CgDeclaration> {
        val declaration = CgDeclaration(variableType, baseVariableName.toVarName(), initializer.resolve())
        val variable = declaration.variable
        updateVariableScope(variable)
        return variable to declaration
    }

    private fun constructArray(arrayModel: UtArrayModel, baseName: String?): CgVariable {
        val elementType = (arrayModel.classId.elementClassId ?: jsUndefinedClassId) as JsClassId
        val elementModels = (0 until arrayModel.length).map {
            arrayModel.stores.getOrDefault(it, arrayModel.constModel)
        }

        val allPrimitives = elementModels.all { it is JsPrimitiveModel }
        val allNulls = elementModels.all { it is JsNullModel }
        // we can use array initializer if all elements are primitives or all of them are null,
        // and the size of an array is not greater than the fixed maximum size
        val canInitWithValues = (allPrimitives || allNulls) && elementModels.size <= MAX_ARRAY_INITIALIZER_SIZE

        val initializer = if (canInitWithValues) {
            val elements = elementModels.map { model ->
                when (model) {
                    is JsPrimitiveModel -> model.value.resolve()
                    is UtNullModel -> null.resolve()
                    else -> error("Non primitive or null model $model is unexpected in array initializer")
                }
            }
            CgArrayInitializer(arrayModel.classId, elementType, elements)
        } else {
            CgAllocateArray(arrayModel.classId, elementType, arrayModel.length)
        }

        val array = newVar(arrayModel.classId, baseName) { initializer }
        val arrayModelId = context.getIdByModel(arrayModel)

        valueByModelId[arrayModelId] = array

        if (canInitWithValues) {
            return array
        }

        if (arrayModel.length <= 0) return array
        if (arrayModel.length == 1) {
            // take first element value if it is present, otherwise use default value from model
            val elementModel = arrayModel[0]
            if (elementModel isNotJsDefaultValueOf elementType) {
                array.setArrayElement(0, getOrCreateVariable(elementModel))
            }
        } else {
            val indexedValuesFromStores =
                if (arrayModel.stores.size == arrayModel.length) {
                    // do not use constModel because stores fully cover array
                    arrayModel.stores.entries.filter { (_, element) -> element isNotJsDefaultValueOf elementType }
                } else {
                    // fill array if constModel is not default type value
                    if (arrayModel.constModel isNotJsDefaultValueOf elementType) {
                        val defaultVariable = getOrCreateVariable(arrayModel.constModel, "defaultValue")
                        basicForLoop(arrayModel.length) { i ->
                            array.setArrayElement(i, defaultVariable)
                        }
                    }

                    // choose all not default values
                    val defaultValue = if (arrayModel.constModel isJsDefaultValueOf elementType) {
                        arrayModel.constModel
                    } else {
                        elementType.defaultValueModel()
                    }
                    arrayModel.stores.entries.filter { (_, element) -> element != defaultValue }
                }

            // set all values from stores manually
            indexedValuesFromStores
                .sortedBy { it.key }
                .forEach { (index, element) -> array.setArrayElement(index, getOrCreateVariable(element)) }
        }

        return array
    }

    private fun String.toVarName(): String = nameGenerator.variableName(this)
}
