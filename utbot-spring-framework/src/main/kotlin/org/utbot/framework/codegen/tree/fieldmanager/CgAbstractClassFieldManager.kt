package org.utbot.framework.codegen.tree.fieldmanager

import org.utbot.framework.codegen.domain.UtModelWrapper
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.AnnotationTarget
import org.utbot.framework.codegen.domain.models.CgDeclaration
import org.utbot.framework.codegen.domain.models.CgFieldDeclaration
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.domain.models.builders.TypedModelWrappers
import org.utbot.framework.codegen.tree.CgComponents
import org.utbot.framework.codegen.tree.CgSpringVariableConstructor
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel

abstract class CgAbstractClassFieldManager(context: CgContext) :
    CgClassFieldManager,
    CgContextOwner by context {

    val annotatedModels: MutableSet<UtModelWrapper> = mutableSetOf()
    protected val modelGroupsProvider = ModelGroupsProvider(context)

    fun findCgValueByModel(model: UtModel, setOfModels: Set<UtModelWrapper>?): CgValue? {
        val key = setOfModels?.find { it == model.wrap() } ?: return null
        return valueByUtModelWrapper[key]
    }

    protected abstract fun fieldWithAnnotationIsRequired(classId: ClassId): Boolean

    protected fun constructFieldsWithAnnotation(modelWrappers: Set<UtModelWrapper>): List<CgFieldDeclaration> {
        val groupedModelsByClassId = modelWrappers.groupByClassId()
        val annotation = statementConstructor.addAnnotation(annotationType, AnnotationTarget.Field)

        val constructedDeclarations = mutableListOf<CgFieldDeclaration>()
        for ((classId, modelWrappers) in groupedModelsByClassId) {

            val modelWrapper = modelWrappers.firstOrNull() ?: continue
            val model = modelWrapper.model

            val fieldWithAnnotationIsRequired = fieldWithAnnotationIsRequired(model.classId)
            if (!fieldWithAnnotationIsRequired) {
                continue
            }

            val baseVarName = constructBaseVarName(model)

            /*
            * `withNameScope` is used to avoid saving names for sub-models of model.
            *
            * Different models from different executions may have the same id.
            * Therefore, when creating a variable for a new class field and using existing `currentTestSetId` and `currentExecutionId`,
            * field`s `model.wrap()` may match one of the values in `annotatedModels`.
            * To avoid false matches when creating a variable for a new class field, `withTestSetIdScope(-1)` and `withExecutionIdScope(-1)` are used.
            */
            val createdVariable = withNameScope {
                withTestSetIdScope(-1) {
                    withExecutionIdScope(-1) {
                        variableConstructor.getOrCreateVariable(model, baseVarName) as? CgVariable
                            ?: error("`CgVariable` cannot be constructed from a $model model")
                    }
                }
            }
            existingVariableNames.add(createdVariable.name)

            val declaration = CgDeclaration(classId, variableName = createdVariable.name, initializer = null)

            constructedDeclarations += CgFieldDeclaration(
                ownerClassId = currentTestClass,
                declaration,
                annotation
            )

            modelWrappers.forEach { modelWrapper ->
                valueByUtModelWrapper[modelWrapper] = createdVariable
                annotatedModels += modelWrapper
            }
        }

        return constructedDeclarations
    }

    protected open fun constructBaseVarName(model: UtModel): String? = nameGenerator.nameFrom(model.classId)

    private fun Set<UtModelWrapper>.groupByClassId(): TypedModelWrappers {
        val classModels = mutableMapOf<ClassId, Set<UtModelWrapper>>()

        for (modelGroup in this.groupBy { it.model.classId }) {
            classModels[modelGroup.key] = modelGroup.value.toSet()
        }

        return classModels
    }

    protected val variableConstructor: CgSpringVariableConstructor by lazy {
        CgComponents.getVariableConstructorBy(context) as CgSpringVariableConstructor
    }
    protected val nameGenerator = CgComponents.getNameGeneratorBy(context)
    protected val statementConstructor = CgComponents.getStatementConstructorBy(context)
}