package org.utbot.instrumentation.instrumentation.execution.constructors

import org.mockito.Mockito
import org.mockito.stubbing.Answer
import org.objectweb.asm.Type
import org.utbot.common.Reflection
import org.utbot.common.invokeCatching
import org.utbot.common.withAccessibility
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.DirectFieldAccessId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtAutowiredStateBeforeModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.UtDirectGetFieldModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtStatementCallModel
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.util.anyInstance
import org.utbot.framework.plugin.api.util.constructor
import org.utbot.framework.plugin.api.util.constructor.CapturedArgument
import org.utbot.framework.plugin.api.util.constructor.constructLambda
import org.utbot.framework.plugin.api.util.constructor.constructStaticLambda
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.jField
import org.utbot.framework.plugin.api.util.method
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.instrumentation.instrumentation.execution.mock.InstanceMockController
import org.utbot.instrumentation.instrumentation.execution.mock.InstrumentationContext
import org.utbot.instrumentation.instrumentation.execution.mock.MethodMockController
import org.utbot.instrumentation.instrumentation.execution.mock.MockController
import org.utbot.instrumentation.instrumentation.execution.mock.SpringInstrumentationContext
import org.utbot.instrumentation.process.runSandbox
import java.lang.reflect.Modifier
import java.util.*
import kotlin.reflect.KClass

/**
 * Constructs values (including mocks) from models.
 *
 * Uses model->constructed object reference-equality cache.
 *
 * This class is based on `ValueConstructor.kt`. The main difference is the ability to create mocked objects and mock
 * static methods.
 *
 * Note that `clearState` was deleted!
 */
// TODO: JIRA:1379 -- Refactor ValueConstructor and MockValueConstructor
class MockValueConstructor(
    private val instrumentationContext: InstrumentationContext
) {
    private val classLoader: ClassLoader
        get() = utContext.classLoader

    val objectToModelCache: IdentityHashMap<Any, UtModel>
        get() {
            val objectToModel = IdentityHashMap<Any, UtModel>()
            constructedObjects.forEach { (model, obj) ->
                objectToModel[obj] = model
            }
            return objectToModel
        }

    // TODO: JIRA:1379 -- replace UtReferenceModel with Int
    private val constructedObjects = HashMap<UtReferenceModel, Any>()

    /**
     * Controllers contain info about mocked methods and have to be closed to restore initial state.
     */
    private val controllers = mutableListOf<MockController>()

    fun constructMethodParameters(models: List<UtModel>): List<UtConcreteValue<*>> =
        models.mapIndexed { _, model -> construct(model) }

    fun constructStatics(staticsBefore: Map<FieldId, UtModel>): Map<FieldId, UtConcreteValue<*>> =
        staticsBefore.mapValues { (_, model) -> construct(model) }

    /**
     * Main construction method.
     *
     * Takes care of nulls. Does not use cache, instead construct(Object/Array/List/FromAssembleModel) method
     * uses cache directly.
     *
     * Takes mock creation context (possible mock target) to create mock if required.
     */
    private fun construct(model: UtModel): UtConcreteValue<*> =
        when (model) {
            is UtNullModel -> UtConcreteValue(null, model.classId.jClass)
            is UtPrimitiveModel -> UtConcreteValue(model.value, model.classId.jClass)
            is UtEnumConstantModel -> UtConcreteValue(constructEnum(model))
            is UtClassRefModel -> UtConcreteValue(model.value)
            is UtCompositeModel -> UtConcreteValue(constructObject(model), model.classId.jClass)
            is UtArrayModel -> UtConcreteValue(constructArray(model))
            is UtAssembleModel -> UtConcreteValue(constructFromAssembleModel(model), model.classId.jClass)
            is UtLambdaModel -> UtConcreteValue(constructFromLambdaModel(model))
            is UtVoidModel -> UtConcreteValue(Unit)
            is UtAutowiredStateBeforeModel -> UtConcreteValue(constructFromAutowiredModel(model))
            // PythonModel, JsUtModel may be here
            else -> throw UnsupportedOperationException("UtModel $model cannot construct UtConcreteValue")
        }

    /**
     * Constructs an Enum<*> instance by model, uses reference-equality cache.
     */
    private fun constructEnum(model: UtEnumConstantModel): Any {
        constructedObjects[model]?.let { return it }
        constructedObjects[model] = model.value
        return model.value
    }

    /**
     * Constructs object by model, uses reference-equality cache.
     *
     * Returns null for mock cause cannot instantiate it.
     */
    private fun constructObject(model: UtCompositeModel): Any {
        constructedObjects[model]?.let { return it }

        val javaClass = javaClass(model.classId)

        val classInstance = if (!model.isMock) {
            val notMockInstance = javaClass.anyInstance

            constructedObjects[model] = notMockInstance
            notMockInstance
        } else {
            val concreteValues = model.mocks.mapValues { mutableListOf<Any?>() }
            val mockInstance = generateMockitoMock(javaClass, concreteValues)

            constructedObjects[model] = mockInstance

            concreteValues.forEach { (executableId, valuesList) ->
                val mockModels = model.mocks.getValue(executableId)
                // If model is unit, then null should be returned (this model has to be already constructed).
                val constructedValues = mockModels.map { model -> construct(model).value.takeIf { it != Unit } }
                valuesList.addAll(constructedValues)
            }

            mockInstance
        }

        model.fields.forEach { (fieldId, fieldModel) ->
            val declaredField = fieldId.jField
            val accessible = declaredField.isAccessible
            declaredField.isAccessible = true

            check(Reflection.isModifiersAccessible())

            val value = construct(fieldModel).value
            val instance = if (Modifier.isStatic(declaredField.modifiers)) null else classInstance
            declaredField.set(instance, value)
            declaredField.isAccessible = accessible
        }

        return classInstance
    }

    private fun generateMockitoAnswer(concreteValues: Map<ExecutableId, List<Any?>>): Answer<*> {
        val pointers = concreteValues.mapValues { (_, _) -> 0 }.toMutableMap()
        return Answer { invocation ->
            with(invocation.method) {
                pointers[executableId].let { pointer ->
                    concreteValues[executableId].let { values ->
                        if (pointer != null && values != null && pointer < values.size) {
                            pointers[executableId] = pointer + 1
                            values[pointer]
                        } else {
                            invocation.callRealMethod()
                        }
                    }
                }
            }
        }
    }

    private fun generateMockitoMock(clazz: Class<*>, concreteValues: Map<ExecutableId, List<Any?>>): Any {
        val answer = generateMockitoAnswer(concreteValues)
        return Mockito.mock(clazz, answer)
    }

    private fun computeConcreteValuesForMethods(
        methodToValues: Map<ExecutableId, List<UtModel>>,
    ): Map<ExecutableId, List<Any?>> = methodToValues.mapValues { (_, models) ->
        models.map { mockAndGet(it) }
    }

    /**
     * Mocks methods on [instance] with supplied [methodToValues].
     *
     * Also add new controllers to [controllers]. Each controller corresponds to one method. If it is a static method, then the controller
     * must be closed. If it is a non-static method and you don't change the mocks behaviour on the passed instance,
     * then the controller doesn't have to be closed
     *
     * @param [instance] must be non-`null` for non-static methods.
     * @param [methodToValues] return values for methods.
     */
    private fun mockMethods(
        instance: Any?,
        methodToValues: Map<ExecutableId, List<UtModel>>,
    ) {
        controllers += computeConcreteValuesForMethods(methodToValues).map { (method, values) ->
            if (method !is MethodId) {
                throw IllegalArgumentException("Expected MethodId, but got: $method")
            }
            MethodMockController(
                method.classId.jClass,
                method.method,
                instance,
                values,
                instrumentationContext
            )
        }

    }

    /**
     * Mocks static methods according to instrumentations.
     */
    fun mockStaticMethods(
        instrumentations: List<UtStaticMethodInstrumentation>,
    ) {
        val methodToValues = instrumentations.associate { it.methodId as ExecutableId to it.values }
        mockMethods(null, methodToValues)
    }

    /**
     * Mocks new instances according to instrumentations
     */
    fun mockNewInstances(
        instrumentations: List<UtNewInstanceInstrumentation>,
    ) {
        controllers += instrumentations.map { mock ->
            InstanceMockController(
                mock.classId,
                mock.instances.map { mockAndGet(it) },
                mock.callSites.map { Type.getType(it.jClass).internalName }.toSet()
            )
        }
    }

    /**
     * Constructs array by model.
     *
     * Supports arrays of primitive, arrays of arrays and arrays of objects.
     *
     * Note: does not check isNull, but if isNull set returns empty array because for null array length set to 0.
     */
    private fun constructArray(model: UtArrayModel): Any {
        constructedObjects[model]?.let { return it }

        with(model) {
            val elementClassId = classId.elementClassId ?: error(
                "Provided incorrect UtArrayModel without elementClassId. ClassId: ${model.classId}, model: $model"
            )
            return when (elementClassId.jvmName) {
                "B" -> ByteArray(length) { primitive(constModel) }.apply {
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }.also { constructedObjects[model] = it }
                "S" -> ShortArray(length) { primitive(constModel) }.apply {
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }.also { constructedObjects[model] = it }
                "C" -> CharArray(length) { primitive(constModel) }.apply {
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }.also { constructedObjects[model] = it }
                "I" -> IntArray(length) { primitive(constModel) }.apply {
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }.also { constructedObjects[model] = it }
                "J" -> LongArray(length) { primitive(constModel) }.apply {
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }.also { constructedObjects[model] = it }
                "F" -> FloatArray(length) { primitive(constModel) }.apply {
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }.also { constructedObjects[model] = it }
                "D" -> DoubleArray(length) { primitive(constModel) }.apply {
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }.also { constructedObjects[model] = it }
                "Z" -> BooleanArray(length) { primitive(constModel) }.apply {
                    stores.forEach { (index, model) -> this[index] = primitive(model) }
                }.also { constructedObjects[model] = it }
                else -> {
                    val javaClass = javaClass(elementClassId)
                    val instance = java.lang.reflect.Array.newInstance(javaClass, length) as Array<*>
                    constructedObjects[model] = instance
                    for (i in instance.indices) {
                        val elementModel = stores[i] ?: constModel
                        val value = construct(elementModel).value
                        try {
                            java.lang.reflect.Array.set(instance, i, value)
                        } catch (iae:IllegalArgumentException) {
                            throw IllegalArgumentException(
                                iae.message + " array: ${instance.javaClass.name}; value: ${value?.javaClass?.name}" , iae
                            )
                        }
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
        val result = updateWithStatementCallModel(instantiationExecutableCall)
        checkNotNull(result) {
            "Tracked instance can't be null for call ${instantiationExecutableCall.statement} in model $assembleModel"
        }
        constructedObjects[assembleModel] = result

        assembleModel.modificationsChain.forEach { statementModel ->
            when (statementModel) {
                is UtStatementCallModel -> updateWithStatementCallModel(statementModel)
                is UtDirectSetFieldModel -> updateWithDirectSetFieldModel(statementModel)
            }
        }

        return constructedObjects[assembleModel] ?: error("Can't assemble model: $assembleModel")
    }

    private fun constructFromLambdaModel(lambdaModel: UtLambdaModel): Any {
        constructedObjects[lambdaModel]?.let { return it }
        // A class representing a functional interface.
        val samType: Class<*> = lambdaModel.samType.jClass
        // A class where the lambda is declared.
        val declaringClass: Class<*> = lambdaModel.declaringClass.jClass
        // A name of the synthetic method that represents a lambda.
        val lambdaName = lambdaModel.lambdaName

        val lambda = if (lambdaModel.lambdaMethodId.isStatic) {
            val capturedArguments = lambdaModel.capturedValues
                .map { model -> CapturedArgument(type = model.classId.jClass, value = value(model)) }
                .toTypedArray()
            constructStaticLambda(samType, declaringClass, lambdaName, *capturedArguments)
        } else {
            val capturedReceiverModel = lambdaModel.capturedValues.firstOrNull()
                ?: error("Non-static lambda must capture `this` instance, so there must be at least one captured value")

            // Values that the given lambda has captured.
            val capturedReceiver = value(capturedReceiverModel)
            val capturedArguments = lambdaModel.capturedValues.subList(1, lambdaModel.capturedValues.size)
                .map { model -> CapturedArgument(type = model.classId.jClass, value = value(model)) }
                .toTypedArray()
            constructLambda(samType, declaringClass, lambdaName, capturedReceiver, *capturedArguments)
        }
        constructedObjects[lambdaModel] = lambda
        return lambda
    }

    private fun constructFromAutowiredModel(autowiredModel: UtAutowiredStateBeforeModel): Any {
        val springInstrumentationContext = instrumentationContext as SpringInstrumentationContext
        autowiredModel.repositoriesContent.forEach { repositoryContent ->
            val repository = springInstrumentationContext.getBean(repositoryContent.repositoryBeanName)
            repositoryContent.entityModels.forEach { entityModel ->
                construct(entityModel).value?.let { entity ->
                    springInstrumentationContext.saveToRepository(repository, entity)
                }
            }
        }
        return springInstrumentationContext.getBean(autowiredModel.beanName)
    }

    /**
     * Updates instance state with [callModel] invocation.
     *
     * @return the result of [callModel] invocation
     */
    private fun updateWithStatementCallModel(callModel: UtStatementCallModel): Any? {
        when (callModel) {
            is UtExecutableCallModel -> {
                val executable = callModel.executable
                val instanceValue = callModel.instance?.let { value(it) }
                val params = callModel.params.map { value(it) }

                return when (executable) {
                    is MethodId -> executable.call(params, instanceValue)
                    is ConstructorId -> executable.call(params)
                }
            }
            is UtDirectGetFieldModel -> {
                val fieldAccess = callModel.fieldAccess
                val instanceValue = value(callModel.instance)

                return fieldAccess.get(instanceValue)
            }
        }
    }

    /**
     * Updates instance with [UtDirectSetFieldModel] execution.
     */
    private fun updateWithDirectSetFieldModel(directSetterModel: UtDirectSetFieldModel) {
        val instanceModel = directSetterModel.instance
        val instance = value(instanceModel)

        val fieldModel = directSetterModel.fieldModel

        val field = directSetterModel.fieldId.jField
        val isAccessible = field.isAccessible

        try {
            //set field accessible to support protected or package-private direct setters
            field.isAccessible = true

            //construct and set the value
            val fieldValue = construct(fieldModel).value
            field.set(instance, fieldValue)
        } finally {
            //restore accessibility property of the field
            field.isAccessible = isAccessible
        }
    }

    /**
     * Constructs value from [UtModel].
     */
    private fun value(model: UtModel) = construct(model).value

    private fun mockAndGet(model: UtModel): Any? {
        return construct(model).value
    }

    private fun MethodId.call(args: List<Any?>, instance: Any?): Any? =
        method.runSandbox {
            invokeCatching(obj = instance, args = args).getOrThrow()
        }

    private fun ConstructorId.call(args: List<Any?>): Any? =
        constructor.runSandbox {
            newInstance(*args.toTypedArray())
        }

    private fun DirectFieldAccessId.get(instance: Any?): Any {
        val field = fieldId.jField
        return field.runSandbox {
            field.get(instance)
        }
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

    fun resetMockedMethods() {
        controllers.forEach { it.close() }
    }
}