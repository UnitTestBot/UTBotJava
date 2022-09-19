package org.utbot.engine

import org.utbot.common.invokeCatching
import org.utbot.framework.plugin.api.ClassId
import org.utbot.engine.util.lambda.CapturedArgument
import org.utbot.engine.util.lambda.constructLambda
import org.utbot.engine.util.lambda.constructStaticLambda
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.FieldMockTarget
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.MockId
import org.utbot.framework.plugin.api.MockInfo
import org.utbot.framework.plugin.api.MockTarget
import org.utbot.framework.plugin.api.ObjectMockTarget
import org.utbot.framework.plugin.api.ParameterMockTarget
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtMockValue
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.UtValueExecution
import org.utbot.framework.plugin.api.UtValueExecutionState
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.util.constructor
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.jField
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.method
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.util.anyInstance
import org.utbot.instrumentation.process.runSandbox
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

/**
 * Constructs values from models.
 *
 * Uses model->constructed object reference-equality cache.
 */
// TODO: JIRA:1379 -- Refactor ValueConstructor and MockValueConstructor
class ValueConstructor {
    private val classLoader: ClassLoader
        get() = utContext.classLoader

    // TODO: JIRA:1379 -- replace UtReferenceModel with Int
    private val constructedObjects = HashMap<UtReferenceModel, Any?>()
    private val mockInfo = mutableListOf<MockInfo>()
    private var mockTarget: MockTarget? = null
    private var mockCounter = 0

    private fun clearState() {
        constructedObjects.clear()
        mockInfo.clear()
        mockTarget = null
        mockCounter = 0
    }

    /**
     * Clears state before and after block execution. State contains caches for reference equality.
     */
    private inline fun <T> withCleanState(block: () -> T): T {
        try {
            clearState()
            return block()
        } finally {
            clearState()
        }
    }

    /**
     * Sets mock context (possible mock target) before block execution and restores previous one after block execution.
     */
    private inline fun <T> withMockTarget(target: MockTarget?, block: () -> T): T {
        val old = mockTarget
        try {
            mockTarget = target
            return block()
        } finally {
            mockTarget = old
        }
    }

    /**
     * Constructs values from models.
     */
    fun construct(models: List<UtModel>): List<UtConcreteValue<*>> =
        withCleanState { models.map { construct(it, null) } }

    private fun constructState(state: EnvironmentModels): Pair<UtValueExecutionState, List<MockInfo>> {
        val (thisInstance, params, statics) = state
        val allParams = listOfNotNull(thisInstance) + params

        val (valuesBefore, mocks, staticValues) = constructParamsAndMocks(allParams, statics)

        val (caller, paramsValues) = if (thisInstance != null) {
            valuesBefore.first() to valuesBefore.drop(1)
        } else {
            null to valuesBefore
        }

        return UtValueExecutionState(caller, paramsValues, staticValues) to mocks
    }

    /**
     * Constructs value based execution from model based.
     */
    fun construct(execution: UtExecution): UtValueExecution<*> {
        val (stateBefore, mocks) = constructState(execution.stateBefore)
        val (stateAfter, _) = constructState(execution.stateAfter)
        val returnValue = execution.result.map { construct(listOf(it)).single().value }

        if (execution is UtSymbolicExecution) {
            return UtValueExecution(
                stateBefore,
                stateAfter,
                returnValue,
                execution.path,
                mocks,
                execution.instrumentation,
                execution.summary,
                execution.testMethodName,
                execution.displayName
            )
        } else {
            return UtValueExecution(
                stateBefore,
                stateAfter,
                returnValue,
                emptyList(),
                mocks,
                emptyList(),
                execution.summary,
                execution.testMethodName,
                execution.displayName
            )
        }
    }

    private fun constructParamsAndMocks(
        models: List<UtModel>,
        statics: Map<FieldId, UtModel>
    ): ConstructedValues =
        withCleanState {
            val values = models.mapIndexed { index, model ->
                val target = mockTarget(model) { ParameterMockTarget(model.classId.name, index) }
                construct(model, target)
            }

            val staticValues = mutableMapOf<FieldId, UtConcreteValue<*>>()

            statics.forEach { (field, model) ->
                val target = FieldMockTarget(model.classId.name, field.declaringClass.name, owner = null, field.name)
                staticValues += field to construct(model, target)
            }

            ConstructedValues(values, mockInfo.toList(), staticValues)
        }

    /**
     * Main construction method.
     *
     * Takes care of nulls. Does not use cache, instead construct(Object/Array/List/FromAssembleModel) method
     * uses cache directly.
     *
     * Takes mock creation context (possible mock target) to create mock if required.
     */
    private fun construct(model: UtModel, target: MockTarget?): UtConcreteValue<*> = withMockTarget(target) {
        when (model) {
            is UtNullModel -> UtConcreteValue(null, model.classId.jClass)
            is UtPrimitiveModel -> UtConcreteValue(model.value, model.classId.jClass)
            is UtEnumConstantModel -> UtConcreteValue(model.value)
            is UtClassRefModel -> UtConcreteValue(model.value)
            is UtCompositeModel -> UtConcreteValue(constructObject(model), model.classId.jClass)
            is UtArrayModel -> UtConcreteValue(constructArray(model))
            is UtAssembleModel -> UtConcreteValue(constructFromAssembleModel(model))
            is UtLambdaModel -> UtConcreteValue(constructFromLambdaModel(model))
            is UtVoidModel -> UtConcreteValue(Unit)
        }
    }

    /**
     * Constructs object by model, uses reference-equality cache.
     *
     * Returns null for mock cause cannot instantiate it.
     */
    private fun constructObject(model: UtCompositeModel): Any? {
        val constructed = constructedObjects[model]
        if (constructed != null) {
            return constructed
        }

        this.mockTarget?.let { mockTarget ->
            model.mocks.forEach { (methodId, models) ->
                mockInfo += MockInfo(mockTarget, methodId, models.map { model ->
                    if (model.isMockModel()) {
                        val mockId = MockId("mock${++mockCounter}")
                        // Call to "construct" method still required to collect mock interaction
                        construct(model, ObjectMockTarget(model.classId.name, mockId))
                        UtMockValue(mockId, model.classId.name)
                    } else {
                        construct(model, null)
                    }
                })
            }
        }

        if (model.isMock) {
            return null
        }

        val javaClass = javaClass(model.classId)
        val classInstance = javaClass.anyInstance
        constructedObjects[model] = classInstance

        model.fields.forEach { (fieldId, fieldModel) ->
            val declaredField = fieldId.jField
            val accessible = declaredField.isAccessible

            try {
                declaredField.isAccessible = true

                val modifiersField = Field::class.java.getDeclaredField("modifiers")
                modifiersField.isAccessible = true

                val target = mockTarget(fieldModel) {
                    FieldMockTarget(
                        fieldModel.classId.name,
                        model.classId.name,
                        UtConcreteValue(classInstance),
                        fieldId.name
                    )
                }
                val value = construct(fieldModel, target).value
                val instance = if (Modifier.isStatic(declaredField.modifiers)) null else classInstance
                declaredField.set(instance, value)
            } finally {
                declaredField.isAccessible = accessible
            }
        }

        return classInstance
    }

    /**
     * Constructs array by model.
     *
     * Supports arrays of primitive, arrays of arrays and arrays of objects.
     *
     * Note: does not check isNull, but if isNull set returns empty array because for null array length set to 0.
     */
    private fun constructArray(model: UtArrayModel): Any {
        val constructed = constructedObjects[model]
        if (constructed != null) {
            return constructed
        }

        with(model) {
            val elementClassId = classId.elementClassId!!
            return when (elementClassId.jvmName) {
                "B" -> ByteArray(length) { primitive(constModel) }.apply {
                    constructedObjects[model] = this
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }
                "S" -> ShortArray(length) { primitive(constModel) }.apply {
                    constructedObjects[model] = this
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }
                "C" -> CharArray(length) { primitive(constModel) }.apply {
                    constructedObjects[model] = this
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }
                "I" -> IntArray(length) { primitive(constModel) }.apply {
                    constructedObjects[model] = this
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }
                "J" -> LongArray(length) { primitive(constModel) }.apply {
                    constructedObjects[model] = this
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }
                "F" -> FloatArray(length) { primitive(constModel) }.apply {
                    constructedObjects[model] = this
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }
                "D" -> DoubleArray(length) { primitive(constModel) }.apply {
                    constructedObjects[model] = this
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }
                "Z" -> BooleanArray(length) { primitive(constModel) }.apply {
                    constructedObjects[model] = this
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }
                else -> {
                    val javaClass = javaClass(elementClassId)
                    val instance = java.lang.reflect.Array.newInstance(javaClass, length) as Array<*>
                    constructedObjects[model] = instance
                    for (i in instance.indices) {
                        val elementModel = stores[i] ?: constModel
                        val value = construct(elementModel, null).value
                        java.lang.reflect.Array.set(instance, i, value)
                    }
                    instance
                }
            }
        }
    }

    /**
     * Constructs object with [UtAssembleModel].
     */
    private fun constructFromAssembleModel(assembleModel: UtAssembleModel): Any {
        constructedObjects[assembleModel]?.let { return it }

        val instantiationExecutableCall = assembleModel.instantiationCall
        val result = updateWithExecutableCallModel(instantiationExecutableCall)
        checkNotNull(result) {
            "Tracked instance can't be null for call ${instantiationExecutableCall.executable} in model $assembleModel"
        }
        constructedObjects[assembleModel] = result

        assembleModel.modificationsChain.forEach { statementModel ->
            when (statementModel) {
                is UtExecutableCallModel -> updateWithExecutableCallModel(statementModel)
                is UtDirectSetFieldModel -> updateWithDirectSetFieldModel(statementModel)
            }
        }

        return constructedObjects[assembleModel] ?: error("Can't assemble model: $assembleModel")
    }

    private fun constructFromLambdaModel(lambdaModel: UtLambdaModel): Any {
        // A class representing a functional interface.
        val samType: Class<*> = lambdaModel.samType.jClass
        // A class where the lambda is declared.
        val declaringClass: Class<*> = lambdaModel.declaringClass.jClass
        // A name of the synthetic method that represents a lambda.
        val lambdaName = lambdaModel.lambdaName

        return if (lambdaModel.lambdaMethodId.isStatic) {
            val capturedArguments = lambdaModel.capturedValues
                .map { model -> CapturedArgument(type = model.classId.jClass, value = value(model)) }
                .toTypedArray()
            constructStaticLambda(samType, declaringClass, lambdaName, *capturedArguments)
        } else {
            val capturedReceiverModel = lambdaModel.capturedValues.firstOrNull()
                ?: error("Non-static lambda must capture `this` instance, so there must be at least one captured value")

            // Values that the given lambda has captured.
            val capturedReceiver = value(capturedReceiverModel) ?: error("Captured receiver of lambda must not be null")
            val capturedArguments = lambdaModel.capturedValues.subList(1, lambdaModel.capturedValues.size)
                .map { model -> CapturedArgument(type = model.classId.jClass, value = value(model)) }
                .toTypedArray()
            constructLambda(samType, declaringClass, lambdaName, capturedReceiver, *capturedArguments)
        }
    }

    /**
     * Updates instance state with [callModel] invocation.
     *
     * @return the result of [callModel] invocation
     */
    private fun updateWithExecutableCallModel(
        callModel: UtExecutableCallModel,
    ): Any? {
        val executable = callModel.executable
        val instanceValue = callModel.instance?.let { value(it) }
        val params = callModel.params.map { value(it) }

        val result = when (executable) {
            is MethodId -> executable.call(params, instanceValue)
            is ConstructorId -> executable.call(params)
        }

        return result
    }

    /**
     * Updates instance with [UtDirectSetFieldModel] execution.
     */
    private fun updateWithDirectSetFieldModel(directSetterModel: UtDirectSetFieldModel) {
        val instanceModel = directSetterModel.instance
        val instance = constructedObjects[instanceModel] ?: error("Model $instanceModel is not instantiated")

        val instanceClassId = instanceModel.classId
        val fieldModel = directSetterModel.fieldModel

        val field = directSetterModel.fieldId.jField
        val isAccessible = field.isAccessible

        try {
            //set field accessible to support protected or package-private direct setters
            field.isAccessible = true

            //prepare mockTarget for field if it is a mock
            val mockTarget = mockTarget(fieldModel) {
                FieldMockTarget(
                    fieldModel.classId.name,
                    instanceClassId.name,
                    UtConcreteValue(javaClass(instanceClassId).anyInstance),
                    field.name
                )
            }

            //construct and set the value
            val fieldValue = construct(fieldModel, mockTarget).value
            field.set(instance, fieldValue)
        } finally {
            //restore accessibility property of the field
            field.isAccessible = isAccessible
        }
    }

    /**
     * Constructs value from [UtModel].
     */
    private fun value(model: UtModel) = construct(model, null).value

    private fun MethodId.call(args: List<Any?>, instance: Any?): Any? =
        method.runSandbox {
            invokeCatching(obj = instance, args = args).getOrThrow()
        }

    private fun ConstructorId.call(args: List<Any?>): Any? =
        constructor.runSandbox {
            newInstance(*args.toTypedArray())
        }

    /**
     * Fetches primitive value from NutsModel to create array of primitives.
     */
    private inline fun <reified T> primitive(model: UtModel): T = (model as UtPrimitiveModel).value as T

    private fun javaClass(id: ClassId) = kClass(id).java

    private fun kClass(id: ClassId) =
        if (id.elementClassId != null) {
            arrayClassOf(id.elementClassId!!)
        } else {
            when (id.jvmName) {
                "B" -> Byte::class
                "S" -> Short::class
                "C" -> Char::class
                "I" -> Int::class
                "J" -> Long::class
                "F" -> Float::class
                "D" -> Double::class
                "Z" -> Boolean::class
                else -> classLoader.loadClass(id.name).kotlin
            }
        }

    private fun arrayClassOf(elementClassId: ClassId): KClass<*> =
        if (elementClassId.elementClassId != null) {
            val elementClass = arrayClassOf(elementClassId.elementClassId!!)
            java.lang.reflect.Array.newInstance(elementClass.java, 0)::class
        } else {
            when (elementClassId.jvmName) {
                "B" -> ByteArray::class
                "S" -> ShortArray::class
                "C" -> CharArray::class
                "I" -> IntArray::class
                "J" -> LongArray::class
                "F" -> FloatArray::class
                "D" -> DoubleArray::class
                "Z" -> BooleanArray::class
                else -> {
                    val elementClass = classLoader.loadClass(elementClassId.name)
                    java.lang.reflect.Array.newInstance(elementClass, 0)::class
                }
            }
        }
}

private fun <R> UtExecutionResult.map(transform: (model: UtModel) -> R): Result<R> = when (this) {
    is UtExecutionSuccess -> Result.success(transform(model))
    is UtExecutionFailure -> Result.failure(exception)
}

/**
 * Creates mock target using init lambda if model represents mock or null otherwise.
 */
private fun mockTarget(model: UtModel, init: () -> MockTarget): MockTarget? =
    if (model.isMockModel()) init() else null

data class ConstructedValues(
    val values: List<UtConcreteValue<*>>,
    val mocks: List<MockInfo>,
    val statics: Map<FieldId, UtConcreteValue<*>>
)