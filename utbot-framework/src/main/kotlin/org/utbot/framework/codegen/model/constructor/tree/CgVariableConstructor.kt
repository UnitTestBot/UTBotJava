package org.utbot.framework.codegen.model.constructor.tree

import org.utbot.common.isStatic
import org.utbot.framework.codegen.model.constructor.builtin.forName
import org.utbot.framework.codegen.model.constructor.builtin.setArrayElement
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.constructor.util.CgComponents
import org.utbot.framework.codegen.model.constructor.util.CgStatementConstructor
import org.utbot.framework.codegen.model.constructor.util.MAX_ARRAY_INITIALIZER_SIZE
import org.utbot.framework.codegen.model.constructor.util.arrayInitializer
import org.utbot.framework.codegen.model.constructor.util.get
import org.utbot.framework.codegen.model.constructor.util.isDefaultValueOf
import org.utbot.framework.codegen.model.constructor.util.isNotDefaultValueOf
import org.utbot.framework.codegen.model.constructor.util.typeCast
import org.utbot.framework.codegen.model.tree.CgAllocateArray
import org.utbot.framework.codegen.model.tree.CgAssignment
import org.utbot.framework.codegen.model.tree.CgDeclaration
import org.utbot.framework.codegen.model.tree.CgEnumConstantAccess
import org.utbot.framework.codegen.model.tree.CgExecutableCall
import org.utbot.framework.codegen.model.tree.CgExpression
import org.utbot.framework.codegen.model.tree.CgFieldAccess
import org.utbot.framework.codegen.model.tree.CgGetJavaClass
import org.utbot.framework.codegen.model.tree.CgLiteral
import org.utbot.framework.codegen.model.tree.CgMethodCall
import org.utbot.framework.codegen.model.tree.CgStatement
import org.utbot.framework.codegen.model.tree.CgStaticFieldAccess
import org.utbot.framework.codegen.model.tree.CgValue
import org.utbot.framework.codegen.model.tree.CgVariable
import org.utbot.framework.codegen.model.util.at
import org.utbot.framework.codegen.model.util.canBeSetFrom
import org.utbot.framework.codegen.model.util.fieldThatIsGotWith
import org.utbot.framework.codegen.model.util.fieldThatIsSetWith
import org.utbot.framework.codegen.model.util.inc
import org.utbot.framework.codegen.model.util.isAccessibleFrom
import org.utbot.framework.codegen.model.util.lessThan
import org.utbot.framework.codegen.model.util.nullLiteral
import org.utbot.framework.codegen.model.util.resolve
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.util.classClassId
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.jField
import org.utbot.framework.plugin.api.util.findFieldByIdOrNull
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isPrimitiveWrapperOrString
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.supertypeOfAnonymousClass
import org.utbot.framework.plugin.api.util.wrapperByPrimitive

/**
 * Constructs CgValue or CgVariable given a UtModel
 */
@Suppress("unused")
internal class CgVariableConstructor(val context: CgContext) :
    CgContextOwner by context,
    CgCallableAccessManager by CgComponents.getCallableAccessManagerBy(context),
    CgStatementConstructor by CgComponents.getStatementConstructorBy(context) {

    private val nameGenerator = CgComponents.getNameGeneratorBy(context)
    private val mockFrameworkManager = CgComponents.getMockFrameworkManagerBy(context)

    /**
     * Take already created CgValue or construct either a new [CgVariable] or new [CgLiteral] for the given model.
     *
     * Here we consider reference models and other models separately.
     *
     * It is important, because variables for reference models are constructed differently from the others.
     * The difference is that the reference model variables are put into [valueByModel] cache
     * not after the whole object is set up, but right after the variable has been initialized.
     * For example, when we do `A a = new A()` we already have the variable, and it is stored in [valueByModel].
     * After that, we set all the necessary fields and call all the necessary methods.
     *
     * On the other hand, non-reference model variables are put into [valueByModel] after they have been fully set up.
     *
     * The point of this early caching of reference model variables is that classes may be recursive.
     * For example, we may want to create a looped object: `a.value = a`.
     * If we did not cache the variable `a` on its instantiation, then we would try to create `a` from its model
     * from scratch. Since this object is recursively pointing to itself, this process would lead
     * to a stack overflow. Specifically to avoid this, we cache reference model variables right after
     * their instantiation.
     *
     * We use [valueByModelId] for [UtReferenceModel] by id to not create new variable in case state before
     * was not transformed.
     */
    fun getOrCreateVariable(model: UtModel, name: String? = null): CgValue {
        // name could be taken from existing names, or be specified manually, or be created from generator
        val baseName = name ?: nameGenerator.nameFrom(model.classId)
        return if (model is UtReferenceModel) valueByModelId.getOrPut(model.id) {
            when (model) {
                is UtCompositeModel -> constructComposite(model, baseName)
                is UtAssembleModel -> constructAssemble(model, baseName)
                is UtArrayModel -> constructArray(model, baseName)
                is UtEnumConstantModel -> constructEnumConstant(model, baseName)
                is UtClassRefModel -> constructClassRef(model, baseName)
                is UtLambdaModel -> constructLambda(model, baseName)
            }
        } else valueByModel.getOrPut(model) {
            when (model) {
                is UtNullModel -> nullLiteral()
                is UtPrimitiveModel -> CgLiteral(model.classId, model.value)
                is UtReferenceModel -> error("Unexpected UtReferenceModel: ${model::class}")
                is UtVoidModel -> error("Unexpected UtVoidModel: ${model::class}")
            }
        }
    }

    private fun constructLambda(model: UtLambdaModel, baseName: String): CgVariable {
        val lambdaMethodId = model.lambdaMethodId
        val capturedValues = model.capturedValues
        return newVar(model.samType, baseName) {
            if (lambdaMethodId.isStatic) {
                constructStaticLambda(model, capturedValues)
            } else {
                constructLambda(model, capturedValues)
            }
        }
    }

    private fun constructStaticLambda(model: UtLambdaModel, capturedValues: List<UtModel>): CgMethodCall {
        val capturedArguments = capturedValues.map {
            utilMethodProvider.capturedArgumentConstructorId(getClassOf(it.classId), getOrCreateVariable(it))
        }
        return utilsClassId[buildStaticLambda](
            getClassOf(model.samType),
            getClassOf(model.declaringClass),
            model.lambdaName,
            *capturedArguments.toTypedArray()
        )
    }

    private fun constructLambda(model: UtLambdaModel, capturedValues: List<UtModel>): CgMethodCall {
        require(capturedValues.isNotEmpty()) {
            "Non-static lambda must capture `this` instance, so there must be at least one captured value"
        }
        val capturedThisInstance = getOrCreateVariable(capturedValues.first())
        val capturedArguments = capturedValues
            .subList(1, capturedValues.size)
            .map { utilMethodProvider.capturedArgumentConstructorId(getClassOf(it.classId), getOrCreateVariable(it)) }
        return utilsClassId[buildLambda](
            getClassOf(model.samType),
            getClassOf(model.declaringClass),
            model.lambdaName,
            capturedThisInstance,
            *capturedArguments.toTypedArray()
        )
    }

    private fun constructComposite(model: UtCompositeModel, baseName: String): CgVariable {
        val obj = if (model.isMock) {
            mockFrameworkManager.createMockFor(model, baseName)
        } else {
            val modelType = model.classId
            val variableType = if (modelType.isAnonymous) modelType.supertypeOfAnonymousClass else modelType
            newVar(variableType, baseName) { utilsClassId[createInstance](model.classId.name) }
        }

        valueByModelId[model.id] = obj

        require(obj.type !is BuiltinClassId) {
            "Unexpected BuiltinClassId ${obj.type} found while constructing from composite model"
        }

        for ((fieldId, fieldModel) in model.fields) {
            val field = fieldId.jField
            val variableForField = getOrCreateVariable(fieldModel)
            val fieldFromVariableSpecifiedType = obj.type.findFieldByIdOrNull(fieldId)

            // we cannot set field directly if variable declared type does not have such field
            // or we cannot directly create variable for field with the specified type (it is private, for example)
            // Example:
            // Object heapByteBuffer = createInstance("java.nio.HeapByteBuffer");
            // branchRegisterRequest.byteBuffer = heapByteBuffer;
            // byteBuffer is field of type ByteBuffer and upper line is incorrect
            val canFieldBeDirectlySetByVariableAndFieldTypeRestrictions =
                fieldFromVariableSpecifiedType != null && fieldFromVariableSpecifiedType.type.id == variableForField.type
            if (canFieldBeDirectlySetByVariableAndFieldTypeRestrictions && fieldId.canBeSetFrom(context)) {
                // TODO: check if it is correct to use declaringClass of a field here
                val fieldAccess = if (field.isStatic) CgStaticFieldAccess(fieldId) else CgFieldAccess(obj, fieldId)
                fieldAccess `=` variableForField
            } else {
                // composite models must not have info about static fields, hence only non-static fields are set here
                +utilsClassId[setField](obj, fieldId.declaringClass.name, fieldId.name, variableForField)
            }
        }
        return obj
    }

    private fun constructAssemble(model: UtAssembleModel, baseName: String?): CgValue {
        val instantiationCall = model.instantiationCall
        processInstantiationStatement(model, instantiationCall, baseName)

        for (statementModel in model.modificationsChain) {
            when (statementModel) {
                is UtDirectSetFieldModel -> {
                    val instance = declareOrGet(statementModel.instance)
                    // fields here are supposed to be accessible, so we assign them directly without any checks
                    instance[statementModel.fieldId] `=` declareOrGet(statementModel.fieldModel)
                }
                is UtExecutableCallModel -> {
                    val call = createCgExecutableCallFromUtExecutableCall(statementModel)
                    val equivalentFieldAccess = replaceCgExecutableCallWithFieldAccessIfNeeded(call)
                    if (equivalentFieldAccess != null)
                        +equivalentFieldAccess
                    else
                        +call
                }
            }
        }

        return valueByModelId.getValue(model.id)
    }

    private fun processInstantiationStatement(
        model: UtAssembleModel,
        executableCall: UtExecutableCallModel,
        baseName: String?
    ) {
        val executable = executableCall.executable
        val params = executableCall.params

        val type = when (executable) {
            is MethodId -> executable.returnType
            is ConstructorId -> executable.classId
        }
        // Don't use redundant constructors for primitives and String
        val initExpr = if (isPrimitiveWrapperOrString(type)) {
            cgLiteralForWrapper(params)
        } else {
            createCgExecutableCallFromUtExecutableCall(executableCall)
        }
        newVar(type, model, baseName) {
            initExpr
        }.also { valueByModelId[model.id] = it }
    }


    private fun createCgExecutableCallFromUtExecutableCall(statementModel: UtExecutableCallModel): CgExecutableCall {
        val executable = statementModel.executable
        val params = statementModel.params
        val cgCall = when (executable) {
            is MethodId -> {
                val caller = statementModel.instance?.let { declareOrGet(it) }
                val args = params.map { declareOrGet(it) }
                caller[executable](*args.toTypedArray())
            }
            is ConstructorId -> {
                val args = params.map { declareOrGet(it) }
                executable(*args.toTypedArray())
            }
        }
        return cgCall
    }

    /**
     * If executable is getter/setter that should be syntactically replaced with field access
     * (e.g., getter/setter generated by Kotlin in Kotlin code), this method returns [CgStatement]
     * with which [call] should be replaced.
     *
     * Otherwise, returns null.
     */
    private fun replaceCgExecutableCallWithFieldAccessIfNeeded(call: CgExecutableCall): CgStatement? {
        when (context.codegenLanguage) {
            CodegenLanguage.JAVA -> return null
            CodegenLanguage.KOTLIN -> {
                if (call !is CgMethodCall)
                    return null

                val caller = call.caller ?: return null

                caller.type.fieldThatIsSetWith(call.executableId)?.let {
                    return CgAssignment(caller[it], call.arguments.single())
                }
                caller.type.fieldThatIsGotWith(call.executableId)?.let {
                    require(call.arguments.isEmpty()) {
                        "Method $call was detected as getter for $it, but its arguments list isn't empty"
                    }
                    return caller[it]
                }

                return null
            }
        }
    }

    /**
     * Makes a replacement of constructor call to instantiate a primitive wrapper
     * with direct setting of the value. The reason is that in Kotlin constructors
     * of primitive wrappers are private.
     */
    private fun cgLiteralForWrapper(params: List<UtModel>): CgLiteral {
        val paramModel = params.singleOrNull()
        require(paramModel is UtPrimitiveModel) { "Incorrect param models for primitive wrapper" }

        val classId = wrapperByPrimitive[paramModel.classId]
            ?: if (paramModel.classId == stringClassId) {
                stringClassId
            } else {
                error("${paramModel.classId} is not a primitive wrapper or a string")
            }

        return CgLiteral(classId, paramModel.value)
    }

    private fun constructArray(arrayModel: UtArrayModel, baseName: String?): CgVariable {
        val elementType = arrayModel.classId.elementClassId!!
        val elementModels = (0 until arrayModel.length).map {
            arrayModel.stores.getOrDefault(it, arrayModel.constModel)
        }

        val allPrimitives = elementModels.all { it is UtPrimitiveModel }
        val allNulls = elementModels.all { it is UtNullModel }
        // we can use array initializer if all elements are primitives or all of them are null,
        // and the size of an array is not greater than the fixed maximum size
        val canInitWithValues = (allPrimitives || allNulls) && elementModels.size <= MAX_ARRAY_INITIALIZER_SIZE

        val initializer = if (canInitWithValues) {
            val elements = elementModels.map { model ->
                when (model) {
                    is UtPrimitiveModel -> model.value.resolve()
                    is UtNullModel -> null.resolve()
                    else -> error("Non primitive or null model $model is unexpected in array initializer")
                }
            }
            arrayInitializer(arrayModel.classId, elementType, elements)
        } else {
            CgAllocateArray(arrayModel.classId, elementType, arrayModel.length)
        }

        val array = newVar(arrayModel.classId, baseName) { initializer }
        valueByModelId[arrayModel.id] = array

        if (canInitWithValues) {
            return array
        }

        if (arrayModel.length <= 0) return array
        if (arrayModel.length == 1) {
            // take first element value if it is present, otherwise use default value from model
            val elementModel = arrayModel[0]
            if (elementModel isNotDefaultValueOf elementType) {
                array.setArrayElement(0, getOrCreateVariable(elementModel))
            }
        } else {
            val indexedValuesFromStores =
                if (arrayModel.stores.size == arrayModel.length) {
                    // do not use constModel because stores fully cover array
                    arrayModel.stores.entries.filter { (_, element) -> element isNotDefaultValueOf elementType }
                } else {
                    // fill array if constModel is not default type value
                    if (arrayModel.constModel isNotDefaultValueOf elementType) {
                        val defaultVariable = getOrCreateVariable(arrayModel.constModel, "defaultValue")
                        basicForLoop(arrayModel.length) { i ->
                            array.setArrayElement(i, defaultVariable)
                        }
                    }

                    // choose all not default values
                    val defaultValue = if (arrayModel.constModel isDefaultValueOf elementType) {
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

    // TODO: cannot be used now but will be useful in case of storing stores in generated code
    /**
     * Splits sorted by indices pairs of index and value from stores to continuous by index chunks
     * [indexedValuesFromStores] have to be sorted by key
     */
    private fun splitSettingFromStoresToForLoops(
        array: CgVariable,
        indexedValuesFromStores: List<MutableMap.MutableEntry<Int, UtModel>>
    ) {
        val ranges = mutableListOf<IntRange>()

        var start = 0
        for (i in 0 until indexedValuesFromStores.lastIndex) {
            if (indexedValuesFromStores[i + 1].key - indexedValuesFromStores[i].key > 1) {
                ranges += start..i
                start = i + 1
            }
            if (i == indexedValuesFromStores.lastIndex - 1) {
                ranges += start..indexedValuesFromStores.lastIndex
            }
        }

        for (range in ranges) {
            // IntRange stores end inclusively but sublist takes it exclusively
            setStoresRange(array, indexedValuesFromStores.subList(range.first, range.last + 1))
        }
    }

    /**
     * [indexedValuesFromStores] have to be continuous sorted range
     */
    private fun setStoresRange(
        array: CgVariable,
        indexedValuesFromStores: List<MutableMap.MutableEntry<Int, UtModel>>
    ) {
        if (indexedValuesFromStores.size < 3) {
            // range is too small, better set manually
            indexedValuesFromStores.forEach { (index, element) ->
                array.setArrayElement(index, getOrCreateVariable(element))
            }
        } else {
            val minIndex = indexedValuesFromStores.first().key
            val maxIndex = indexedValuesFromStores.last().key

            var indicesIndex = 0
            // we use until form of for loop so need to shift upper border
            basicForLoop(start = minIndex, until = maxIndex + 1) { i ->
                // use already sorted indices
                val (_, value) = indexedValuesFromStores[indicesIndex++]
                array.setArrayElement(i, getOrCreateVariable(value))
            }
        }
    }

    private fun constructEnumConstant(model: UtEnumConstantModel, baseName: String?): CgVariable {
        return newVar(model.classId, baseName) {
            CgEnumConstantAccess(model.classId, model.value.name)
        }
    }

    private fun constructClassRef(model: UtClassRefModel, baseName: String?): CgVariable {
        val classId = model.value.id
        val init = if (classId.isAccessibleFrom(testClassPackageName)) {
            CgGetJavaClass(classId)
        } else {
            classClassId[forName](classId.name)
        }

        return newVar(Class::class.id, baseName) { init }
    }

    /**
     * Either declares a new variable or gets it from context's cache
     * Returns the obtained variable
     */
    private fun declareOrGet(model: UtModel): CgValue = valueByModel[model] ?: getOrCreateVariable(model)

    private fun basicForLoop(start: Any, until: Any, body: (i: CgExpression) -> Unit) {
        forLoop {
            val (i, init) = loopInitialization(intClassId, "i", start.resolve())
            initialization = init
            condition = i lessThan until.resolve()
            update = i.inc()
            statements = block { body(i) }
        }
    }

    /**
     * A for-loop performing 'n' iterations starting with 0
     *
     * for (int i = 0; i < n; i++) {
     *     ...
     * }
     */
    private fun basicForLoop(until: Any, body: (i: CgExpression) -> Unit) {
        basicForLoop(start = 0, until, body)
    }

    /**
     * Create loop initializer expression
     */
    @Suppress("SameParameterValue")
    internal fun loopInitialization(
        variableType: ClassId,
        baseVariableName: String,
        initializer: Any?
    ): Pair<CgVariable, CgDeclaration> {
        val declaration = CgDeclaration(variableType, baseVariableName.toVarName(), initializer.resolve())
        val variable = declaration.variable
        updateVariableScope(variable)
        return variable to declaration
    }

    /**
     * @receiver must represent a variable containing an array value.
     * If an array was created with reflection, then the variable is of [Object] type.
     * Otherwise, the variable is of the actual array type.
     *
     * Both cases are considered here.
     * If the variable is [Object], we use reflection method to set an element.
     * Otherwise, we set an element directly.
     */
    private fun CgVariable.setArrayElement(index: Any, value: CgValue) {
        val i = index.resolve()
        // we have to use reflection if we cannot easily cast array element to array type
        // (in case array does not have array type (maybe just object) or element is private class)
        if (!type.isArray || (type != value.type && !value.type.isAccessibleFrom(testClassPackageName))) {
            +java.lang.reflect.Array::class.id[setArrayElement](this, i, value)
        } else {
            val arrayElement = if (type == value.type) {
                value
            } else {
                typeCast(type.elementClassId!!, value, isSafetyCast = true)
            }

            this.at(i) `=` arrayElement
        }
    }

    internal fun constructVarName(baseName: String, isMock: Boolean = false): String =
        nameGenerator.variableName(baseName, isMock)

    private fun String.toVarName(): String = nameGenerator.variableName(this)

}