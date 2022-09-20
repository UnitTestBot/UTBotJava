package org.utbot.engine

import org.utbot.common.WorkaroundReason.HACK
import org.utbot.common.WorkaroundReason.MAKE_SYMBOLIC
import org.utbot.common.WorkaroundReason.RUN_CONCRETE
import org.utbot.common.workaround
import org.utbot.engine.MemoryState.CURRENT
import org.utbot.engine.MemoryState.INITIAL
import org.utbot.engine.MemoryState.STATIC_INITIAL
import org.utbot.engine.TypeRegistry.Companion.objectNumDimensions
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtArrayExpressionBase
import org.utbot.engine.pc.UtArraySort
import org.utbot.engine.pc.UtExpression
import org.utbot.engine.pc.UtInt32Sort
import org.utbot.engine.pc.UtSolverStatusSAT
import org.utbot.engine.pc.mkBVConst
import org.utbot.engine.pc.mkBool
import org.utbot.engine.pc.mkByte
import org.utbot.engine.pc.mkChar
import org.utbot.engine.pc.mkDouble
import org.utbot.engine.pc.mkFloat
import org.utbot.engine.pc.mkInt
import org.utbot.engine.pc.mkLong
import org.utbot.engine.pc.mkShort
import org.utbot.engine.pc.select
import org.utbot.engine.pc.store
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.engine.util.statics.concrete.isEnumValuesFieldName
import org.utbot.engine.z3.intValue
import org.utbot.engine.z3.value
import org.utbot.framework.assemble.AssembleModelGenerator
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.SYMBOLIC_NULL_ADDR
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtOverflowFailure
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtSandboxFailure
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.classId
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.constructorId
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.primitiveByWrapper
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.util.nextModelName
import soot.ArrayType
import soot.BooleanType
import soot.ByteType
import soot.CharType
import soot.DoubleType
import soot.FloatType
import soot.IntType
import soot.LongType
import soot.PrimType
import soot.RefType
import soot.Scene
import soot.ShortType
import soot.SootClass
import soot.SootField
import soot.Type
import soot.VoidType
import java.awt.color.ICC_ProfileRGB
import java.io.PrintStream
import java.security.AccessControlContext
import java.security.AccessControlException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

// hack
const val MAX_LIST_SIZE = 10
const val MAX_RESOLVE_LIST_SIZE = 256
const val MAX_STRING_SIZE = 40
internal const val HARD_MAX_ARRAY_SIZE = 256

internal const val PREFERRED_ARRAY_SIZE = 2
internal const val MIN_PREFERRED_INTEGER = -256
internal const val MAX_PREFERRED_INTEGER = 256
internal const val MIN_PREFERRED_CHARACTER = 32
internal const val MAX_PREFERRED_CHARACTER = 127
internal const val MAX_STRING_LENGTH_SIZE_BITS = 8
internal const val MAX_NUM_DIMENSIONS = 4
internal const val MIN_ARRAY_SIZE = 0

typealias Address = Int

/**
 * Constructs values using calculated model. Can construct them for initial and current memory states that reflect
 * initial parameters or current values for concrete call.
 */
class Resolver(
    val hierarchy: Hierarchy,
    private val memory: Memory,
    val typeRegistry: TypeRegistry,
    private val typeResolver: TypeResolver,
    val holder: UtSolverStatusSAT,
    methodUnderTest: ExecutableId,
    private val softMaxArraySize: Int
) {

    private val classLoader: ClassLoader
        get() = utContext.classLoader

    lateinit var state: MemoryState
    private val constructedModels = mutableMapOf<Address, UtModel>()

    private val mockInfos = mutableMapOf<Address, MockInfoEnriched>()
    private val staticFieldMocks = mutableMapOf<FieldId, UtModel>()
    private val instrumentation = mutableListOf<UtInstrumentation>()
    private val requiredInstanceFields = mutableMapOf<Address, Set<FieldId>>()

    private val assembleModelGenerator = AssembleModelGenerator(methodUnderTest.classId.packageName)

    /**
     * Contains FieldId of the static field which is construction at the moment and null of there is no such field.
     * It is used to find initial state for the fieldId in the [Memory.findArray] method.
     */
    private var staticFieldUnderResolving: FieldId? = null

    private fun clearState() {
        constructedModels.clear()

        mockInfos.clear()
        staticFieldMocks.clear()
        instrumentation.clear()
    }

    private fun findConstructedModelOrNull(addr: Address): UtModel? = constructedModels[addr]

    internal fun addConstructedModel(addr: Address, model: UtModel) {
        constructedModels[addr] = model
    }

    private inline fun <T> withMemoryState(state: MemoryState, block: () -> T): T {
        try {
            this.state = state
            clearState()
            return block()
        } finally {
            clearState()
        }
    }

    private inline fun <T> withStaticMemoryState(staticFieldUnderResolving: FieldId, block: () -> T): T {
        return if (state == INITIAL) {
            try {
                this.staticFieldUnderResolving = staticFieldUnderResolving
                this.state = STATIC_INITIAL
                block()
            } finally {
                this.staticFieldUnderResolving = null
                this.state = INITIAL
            }
        } else {
            block()
        }
    }

    /**
     * Returns a symbolic array from the memory by [chunkDescriptor] and [state].
     *
     * Although it just calls [memory]`s method, there is an important difference -- during the resolve
     * process we sometimes have [STATIC_INITIAL] state, which takes memory state at the moment of initialization
     * of the [staticFieldUnderResolving]. Since we cannot have this information outside the resolver,
     * we need to make sure no one can call method from the [memory] directly,
     * without providing [staticFieldUnderResolving].
     *
     * @see Memory.findArray
     */
    fun findArray(
        chunkDescriptor: MemoryChunkDescriptor,
        state: MemoryState,
    ): UtArrayExpressionBase = memory.findArray(chunkDescriptor, state, staticFieldUnderResolving)

    internal fun resolveModels(parameters: List<SymbolicValue>): ResolvedExecution {
        var instrumentation: List<UtInstrumentation> = emptyList()

        val staticsBefore = memory.staticFields().map { (fieldId, states) -> fieldId to states.stateBefore }
        val staticsAfter = memory.staticFields().map { (fieldId, states) -> fieldId to states.stateAfter }

        val modelsBefore = withMemoryState(INITIAL) {
            internalResolveModel(parameters, staticsBefore)
        }

        val modelsAfter = withMemoryState(CURRENT) {
            val resolvedModels = internalResolveModel(parameters, staticsAfter)
            instrumentation = this.instrumentation.toList()
            resolvedModels
        }

        val resolvedExecution = ResolvedExecution(modelsBefore, modelsAfter, instrumentation)
        return assembleModelGenerator.createAssembleModels(resolvedExecution)
    }

    internal fun resolveParametersForNativeCall(parameters: List<SymbolicValue>): List<UtModel> =
        withMemoryState(CURRENT) {
            internalResolveModel(parameters, statics = emptyList()).parameters
        }

    /**
     * Resolves given symbolic values to models deeply, using models cache.
     *
     * Resolves also static field mocks and instrumentation required.
     */
    private fun internalResolveModel(
        parameters: List<SymbolicValue>,
        statics: List<Pair<FieldId, SymbolicValue>>
    ): ResolvedModels {
        collectMocksAndInstrumentation()

        val parameterModels = parameters.map { resolveModel(it) }

        val staticModels = statics.map { (fieldId, value) ->
            withStaticMemoryState(fieldId) {
                staticFieldMocks.getOrElse(fieldId) {
                    resolveModel(value)
                }
            }
        }

        val allStatics = mutableMapOf<FieldId, UtModel>().apply {
            this += staticFieldMocks
            this += staticModels.mapIndexed { i, model -> statics[i].first to model }
        }

        return ResolvedModels(parameterModels, allStatics)
    }

    /**
     * Collects all instance fields (as a mapping from concrete addresses to [FieldId])
     * that should be initialized because their values are read during the execution.
     *
     * If a class is a substitute, corresponding target fields are also added to the mapping.
     *
     * This function must be called before any model construction, otherwise some or all fields may be uninitialized.
     */
    private fun collectRequiredFields() {
        requiredInstanceFields += memory.initializedFields { holder.concreteAddr(it) }
    }

    /**
     * Collects all mock and instrumentation related information, stores it into shared maps.
     */
    private fun collectMocksAndInstrumentation() {
        // Update required field map from the memory (it must be done before any model is constructed)
        collectRequiredFields()

        // Associates mock infos by concrete addresses
        // Sometimes we might have two mocks with the same concrete address, and here we group their mockInfos
        // Then we should merge executables for mocks with the same concrete addresses with respect to the calls order
        val mocks = memory
            .mocks()
            .groupBy { enriched -> holder.concreteAddr(enriched.mockInfo.addr) }
            .map { (address, mockInfos) -> address to mockInfos.mergeExecutables() }

        mockInfos += mocks

        // Collects static fields and instrumentation
        val staticMethodMocks = mutableMapOf<MethodId, List<UtModel>>()

        // Enriches mock info with information from callsToMocks
        memory.mocks().forEach { (mockInfo, executables) ->
            when (mockInfo) {
                // Collects static field mocks differently
                is UtFieldMockInfo -> if (mockInfo.ownerAddr == null) {
                    val field = mockInfo.fieldId
                    val fieldAddr = holder.eval(mockInfo.addr).intValue()
                    staticFieldMocks[field] = resolveMock(fieldAddr)
                }
                is UtStaticObjectMockInfo -> executables.forEach { (executableId, executableInstances) ->
                    staticMethodMocks[executableId as MethodId] = executableInstances.map { resolveModel(it.value) }
                }
                is UtNewInstanceMockInfo, is UtObjectMockInfo, is UtStaticMethodMockInfo -> Unit
            }
        }

        // Collects instrumentation
        val newInstancesInstrumentation = memory.mocks()
            .map { it.mockInfo }
            .filterIsInstance<UtNewInstanceMockInfo>()
            .groupBy { it.classId }
            .map { (classId, instanceMockInfos) ->
                val instances = mutableListOf<UtCompositeModel>()
                val callSites = mutableSetOf<ClassId>()
                for (mockInfo in instanceMockInfos) {
                    val concreteAddr = holder.concreteAddr(mockInfo.addr)
                    instances += resolveMock(concreteAddr) as UtCompositeModel
                    callSites += mockInfo.callSite
                }
                UtNewInstanceInstrumentation(classId, instances, callSites)
            }

        val staticMethodsInstrumentation = staticMethodMocks.map { (methodId, models) ->
            UtStaticMethodInstrumentation(methodId, models)
        }

        instrumentation += newInstancesInstrumentation
        instrumentation += staticMethodsInstrumentation
    }

    // TODO: can we get rid of it?
    private fun resolveMock(concreteAddr: Address): UtModel {
        val mockInfoEnriched = mockInfos.getValue(concreteAddr)
        val mockInfo = mockInfoEnriched.mockInfo

        if (concreteAddr == SYMBOLIC_NULL_ADDR) {
            return UtNullModel(mockInfo.classId)
        }

        findConstructedModelOrNull(concreteAddr)?.let { return it }

        val mockInstanceModel = UtCompositeModel(concreteAddr, mockInfo.classId, isMock = true)
        addConstructedModel(concreteAddr, mockInstanceModel)

        mockInstanceModel.fields += collectFieldModels(
            UtAddrExpression(holder.concreteAddr(mockInfo.addr)),
            Scene.v().getRefType(mockInfo.classId.name)
        )
        mockInstanceModel.mocks += collectMockBehaviour(mockInfoEnriched)

        return mockInstanceModel
    }

    /**
     * Resolves current result (return value).
     */
    fun resolveResult(symResult: SymbolicResult): UtExecutionResult =
        withMemoryState(CURRENT) {
            when (symResult) {
                is SymbolicSuccess -> {
                    collectMocksAndInstrumentation()
                    val model = resolveModel(symResult.value)
                    val modelsAfterAssembling = assembleModelGenerator.createAssembleModels(listOf(model))
                    UtExecutionSuccess(modelsAfterAssembling.getValue(model))
                }
                is SymbolicFailure -> symResult.resolve()
            }
        }

    /**
     * There are four different kinds of UtExecutionFailure and rules to define one:
     * * if exception thrown implicitly, it's UtUnexpectedFail or UtOverflowFailure for ArithmeticException with "overflow"
     * * otherwise if exception checked, it's UtExpectedCheckedThrow
     * * otherwise it depends on inNestedMethod - it's UtUnexpectedUncheckedThrow for nested calls and
     * UtExpectedUncheckedThrow for method under test
     */
    private fun SymbolicFailure.resolve(): UtExecutionFailure {
        val exception = concreteException()
        return if (explicit) {
            UtExplicitlyThrownException(exception, inNestedMethod)
        } else {
            when {
                // TODO SAT-1561
                exception is ArithmeticException && exception.message?.contains("overflow") == true -> UtOverflowFailure(exception)
                exception is AccessControlException -> UtSandboxFailure(exception)
                else -> UtImplicitlyThrownException(exception, inNestedMethod)
            }
        }
    }

    private fun SymbolicFailure.concreteException() = concrete ?: concreteException(symbolic)

    /**
     * Creates concrete exception for symbolic one using conversion through model.
     */
    private fun concreteException(symbolic: SymbolicValue): Throwable =
        ValueConstructor().construct(listOf(resolveModel(symbolic))).single().value as Throwable

    fun resolveModel(value: SymbolicValue): UtModel = when (value) {
        is PrimitiveValue -> resolvePrimitiveValue(value)
        is ReferenceValue -> resolveReferenceValue(value)
    }

    private fun resolvePrimitiveValue(value: PrimitiveValue): UtModel =
        if (value.type == VoidType.v()) {
            UtVoidModel
        } else {
            UtPrimitiveModel(holder.eval(value.expr).value(value.type.unsigned))
        }

    private fun resolveReferenceValue(value: ReferenceValue): UtModel {
        if (value is ArrayValue) {
            return constructArrayModel(value)
        }

        value as ObjectValue

        // Check if we have mock with this address.
        // Unfortunately we cannot do it in constructModel only cause constructTypeOrNull returns null for mocks
        val concreteAddr = holder.concreteAddr(value.addr)
        if (concreteAddr in mockInfos) {
            return resolveMock(concreteAddr)
        }

        val isObjectClassInstance = value.type.isJavaLangObject()
        val numDimensions = holder.findNumDimensionsOrNull(value.addr) ?: objectNumDimensions

        // if the value is not an instance of java.lang.Object, the number of dimensions does not matter
        if (!isObjectClassInstance || numDimensions <= 0) {
            return resolveObject(value)
        }

        // if the value is Object, we have to construct array or an object depending on the number of dimensions
        // it is possible if we had an object and we casted it into array
        val constructedType = holder.constructTypeOrNull(value.addr, value.type) ?: return UtNullModel(value.type.id)
        val typeStorage = TypeStorage(constructedType)

        return if (constructedType is ArrayType) {
            constructArrayModel(ArrayValue(typeStorage, value.addr))
        } else {
            resolveObject(ObjectValue(typeStorage, value.addr))
        }
    }

    private fun resolveObject(objectValue: ObjectValue): UtModel {
        val concreteAddr = holder.concreteAddr(objectValue.addr)
        if (concreteAddr == SYMBOLIC_NULL_ADDR) {
            return UtNullModel(objectValue.type.sootClass.id)
        }

        // TODO to avoid recursive wrappers JIRA:1524
        // TODO check every wrapper and add the cache there.
        findConstructedModelOrNull(concreteAddr)?.let { return it }

        val wrapper = objectValue.asWrapperOrNull
        if (wrapper != null) {
            return if (wrapper is UtMockWrapper) {
                resolveMock(concreteAddr)
            } else {
                wrapper.value(this, objectValue)
            }
        }

        val constructedType = holder.constructTypeOrNull(objectValue.addr, objectValue.type) as? RefType
            ?: return UtNullModel(objectValue.type.id)
        val concreteType =
            typeResolver.findAnyConcreteInheritorIncludingOrDefaultUnsafe(constructedType, objectValue.type)

        resolveWrappersAndOverriddenTypesOrNull(concreteType, objectValue.addr, objectValue.type)?.let { return it }

        return constructModel(objectValue.addr, objectValue.type, concreteType)
    }

    /**
     * [concreteType] might be one of the types that must be either substituted, or removed from the hierarchy.
     * It might happen if there was a java.lang.Object that later were resolved as an instance of [concreteType].
     */
    private fun resolveWrappersAndOverriddenTypesOrNull(concreteType: RefType, addr: UtAddrExpression, defaultType: Type): UtModel? {
        // We need it to construct correct value if we have, e.g., an Object transformed into a String.
        // TODO JIRA:1464
        workaround(HACK) {
            wrapper(concreteType, addr)?.let {
                return resolveModel(it)
            }
        }

        // If we have our type, we construct an instance of the default type.
        // TODO JIRA:1465
        workaround(HACK) {
            if (concreteType.sootClass.isOverridden || concreteType.sootClass.isUtMock) {
                require(defaultType is RefType)

                val fallbackConcreteType =
                    typeResolver.findAnyConcreteInheritorIncludingOrDefaultUnsafe(defaultType, defaultType)
                return constructModel(addr, defaultType, fallbackConcreteType)
            }
        }

        return null
    }

    private fun constructModel(
        addr: UtAddrExpression,
        defaultType: RefType,
        actualType: RefType,
    ): UtModel {
        val concreteAddr = holder.concreteAddr(addr)
        if (concreteAddr == SYMBOLIC_NULL_ADDR) {
            return UtNullModel(defaultType.sootClass.id)
        }

        findConstructedModelOrNull(concreteAddr)?.let { return it }

        if (actualType.className == CLASS_REF_CLASSNAME) {
            return constructClassRef(concreteAddr)
        }

        val sootClass = actualType.sootClass

        if (sootClass.isLambda) {
            return constructLambda(concreteAddr, sootClass).also { lambda ->
                lambda.capturedValues += collectFieldModels(addr, actualType).values
            }
        }

        val clazz = classLoader.loadClass(sootClass.name)

        if (clazz.isEnum) {
            return constructEnum(concreteAddr, actualType, clazz)
        }

        // check if we have mock with this address
        // Unfortunately we cannot do it in constructModel only cause constructTypeOrNull returns null for mocks
        val mockInfo = mockInfos[concreteAddr]

        return tryConstructAssembleModel(concreteAddr, clazz.id, actualType, isMock = mockInfo != null)
            ?: UtCompositeModel(concreteAddr, clazz.id, isMock = mockInfo != null).also { instanceModel ->
                addConstructedModel(concreteAddr, instanceModel)

                instanceModel.fields += collectFieldModels(addr, actualType)
                instanceModel.mocks += collectMockBehaviour(mockInfo)
            }
    }

    private fun tryConstructAssembleModel(
        addr: Int,
        classId: ClassId,
        actualType: RefType,
        isMock: Boolean
    ): UtAssembleModel? {
        return if (isMock || classId !in primitiveByWrapper) {
            null
        } else {
            val primitiveClassId = primitiveByWrapper[classId]!!
            val fields = collectFieldModels(UtAddrExpression(mkInt(addr)), actualType)
            val baseModelName = primitiveClassId.name
            val constructorId = constructorId(classId, primitiveClassId)
            val valueModel = fields[FieldId(classId, "value")] ?: primitiveClassId.defaultValueModel()
            val instantiationCall = UtExecutableCallModel(instance = null, constructorId, listOf(valueModel))
            UtAssembleModel(addr, classId, nextModelName(baseModelName), instantiationCall)
        }
    }

    fun collectFieldModels(addr: UtAddrExpression, actualType: RefType): Map<FieldId, UtModel> {
        val fields = mutableMapOf<FieldId, UtModel>()
        val requiredFields = requiredInstanceFields[holder.concreteAddr(addr)] ?: emptySet()

        // Collect fields with memory chunk ids that should be initialized as they are read during the execution
        val fieldWithChunkIds = typeResolver.findFields(actualType)
            .map { FieldWithChunkId(it, hierarchy.chunkIdForField(actualType, it)) }
            .filter { !it.field.isStatic }
            .filter { it.chunkId in memory.chunkIds }
            .filter { it.field.fieldId in requiredFields }

        for ((field, fieldChunkId) in fieldWithChunkIds) {
            val fieldId = field.fieldId

            val descriptor = MemoryChunkDescriptor(fieldChunkId, actualType, field.type)
            val fieldArray = findArray(descriptor, state)

            val fieldValue = fieldArray.select(addr)
            val model = when (val fieldType = field.type) {
                is RefType -> {
                    val fieldAddr = UtAddrExpression(fieldValue)

                    // Use wrapper if needed
                    val objectInstance = wrapper(fieldType, fieldAddr) ?: ObjectValue(
                        typeResolver.constructTypeStorage(fieldType, useConcreteType = false),
                        fieldAddr
                    )
                    resolveModel(objectInstance)
                }
                is ArrayType -> {
                    val fieldAddr = UtAddrExpression(fieldValue)
                    val arrayInstance = ArrayValue(
                        typeResolver.constructTypeStorage(fieldType, useConcreteType = false),
                        fieldAddr
                    )
                    resolveModel(arrayInstance)
                }
                is PrimType -> UtPrimitiveModel(holder.eval(fieldValue).value(fieldType.unsigned))
                else -> error("Unknown type $fieldType")
            }
            fields[fieldId] = model
        }
        return fields
    }

    private fun collectMockBehaviour(mockInfo: MockInfoEnriched?) =
        mockInfo?.executables.orEmpty().mapValues { (_, values) ->
            values.map { resolveModel(it.value) }
        }

    /**
     * Constructs class reference by modeled class name. In case no name defined, uses [Object] as a fallback.
     *
     * TODO: perhaps we can find field generic info and get its class through [java.lang.reflect.Field.getGenericType]
     */
    private fun constructClassRef(addr: Address): UtClassRefModel {
        val typeArray = findArray(CLASS_REF_TYPE_DESCRIPTOR, state)
        val numDimensionsArray = findArray(CLASS_REF_NUM_DIMENSIONS_DESCRIPTOR, state)

        val addrExpression = UtAddrExpression(addr)
        val typeId = holder.eval(typeArray.select(addrExpression)).intValue()
        val modeledType = typeRegistry.typeByIdOrNull(typeId) ?: OBJECT_TYPE
        val modeledNumDimensions = holder.eval(numDimensionsArray.select(addrExpression)).intValue()

        val classRef = classRefByName(modeledType, modeledNumDimensions)
        val model = UtClassRefModel(addr, CLASS_REF_CLASS_ID, classRef)
        addConstructedModel(addr, model)

        return model
    }

    private fun classRefByName(type: Type, numDimensions: Int): Class<*> {
        require(numDimensions >= 0) {
            "Number of dimensions for ClassRef should be non-negative, but $numDimensions found"
        }

        val constructedType = if (numDimensions == 0) type else type.makeArrayType(numDimensions)
        return constructedType.classId.jClass
    }

    private fun constructLambda(addr: Address, sootClass: SootClass): UtLambdaModel {
        val samType = sootClass.interfaces.singleOrNull()?.id
            ?: error("Lambda must implement single interface, but ${sootClass.interfaces.size} found for ${sootClass.name}")

        val declaringClass = classLoader.loadClass(sootClass.name.substringBefore("\$lambda"))

        // Java compiles lambdas into synthetic methods with specific names.
        // However, Soot represents lambdas as classes.
        // Names of these classes are the modified names of these synthetic methods.
        // Specifically, Soot replaces some `$` signs by `_`, adds two underscores and some number
        // to the end of the synthetic method name to form the name of a SootClass for lambda.
        // For example, given a synthetic method `lambda$foo$1` (lambda declared in method `foo` of class `org.utbot.MyClass`),
        // Soot will treat this lambda as a class named `org.utbot.MyClass$lambda_foo_1__5` (the last number is probably arbitrary, it's not important).
        // Here we obtain the synthetic method name of lambda from the name of its SootClass.
        val lambdaName = sootClass.name
            .let { name ->
                val start = name.indexOf("\$lambda") + 1
                val end = name.lastIndexOf("__")
                name.substring(start, end)
            }
            .let {
                val builder = StringBuilder(it)
                builder[it.indexOfFirst { c -> c == '_' }] = '$'
                builder[it.indexOfLast { c -> c == '_' }] = '$'
                builder.toString()
            }

        return UtLambdaModel(
            id = addr,
            samType = samType,
            declaringClass = declaringClass.id,
            lambdaName = lambdaName
        )
    }

    private fun constructEnum(addr: Address, type: RefType, clazz: Class<*>): UtEnumConstantModel {
        val descriptor = MemoryChunkDescriptor(ENUM_ORDINAL, type, IntType.v())
        val array = findArray(descriptor, state)
        val evaluatedIndex = holder.eval(array.select(mkInt(addr))).value() as Int
        val index = if (evaluatedIndex in clazz.enumConstants.indices) {
            evaluatedIndex
        } else {
            clazz.enumConstants.indices.random()
        }
        val value = clazz.enumConstants[index] as Enum<*>
        val model = UtEnumConstantModel(addr, clazz.id, value)
        addConstructedModel(addr, model)

        return model
    }

    /**
     * Returns true if the [addr] has been touched during the analysis and false otherwise.
     */
    private fun UtSolverStatusSAT.isTouched(addr: UtAddrExpression): Boolean {
        return eval(memory.isTouched(addr)).value() as Boolean
    }

    /**
     * Returns evaluated type by object's [addr] or null if there is no information about evaluated typeId.
     */
    private fun UtSolverStatusSAT.findBaseTypeOrNull(addr: UtAddrExpression): Type? {
        val typeId = eval(typeRegistry.symTypeId(addr)).intValue()
        return typeRegistry.typeByIdOrNull(typeId)
    }

    /**
     * We have a constraint stated that every number of dimensions is in [0..MAX_NUM_DIMENSIONS], so if we have a value
     * from outside of the range, it means that we have never touched the number of dimensions for the given addr.
     */
    private fun UtSolverStatusSAT.findNumDimensionsOrNull(addr: UtAddrExpression): Int? {
        val numDimensions = eval(typeRegistry.symNumDimensions(addr)).intValue()
        return if (numDimensions in 0..MAX_NUM_DIMENSIONS) numDimensions else null
    }

    /**
     * Constructs a type for the addr.
     *
     * There are three options here:
     * * it successfully constructs a type suitable with defaultType and returns it;
     * * it constructs a type that cannot be assigned in a variable with the [defaultType] and we `touched`
     * the [addr] during the analysis. In such case the method returns [defaultType] as a result;
     * * it constructs a type that cannot be assigned in a variable with the [defaultType] and we did **not** `touched`
     * the [addr] during the analysis. It means we can create [UtNullModel] to represent such element. In such case
     * the method returns null as the result.
     *
     * @see Memory.touchedAddresses
     * @see Traverser.touchAddress
     */
    private fun UtSolverStatusSAT.constructTypeOrNull(addr: UtAddrExpression, defaultType: Type): Type? {
        return constructTypeOrNull(addr, defaultType, isTouched(addr))
    }

    private fun UtSolverStatusSAT.constructTypeOrNull(addr: UtAddrExpression, defaultType: Type, isTouched: Boolean): Type? {
        val evaluatedType = findBaseTypeOrNull(addr) ?: return if (isTouched) defaultType else null
        val numDimensions = findNumDimensionsOrNull(addr) ?: defaultType.numDimensions

        // If we have numDimensions greater than zero, we have to check if the object is a java.lang.Object
        // that is actually an instance of some array (e.g., Object -> Int[])
        if (defaultType.isJavaLangObject() && numDimensions > 0) {
            return evaluatedType.makeArrayType(numDimensions)
        }

        // If it does not, the numDimension must be exactly the same as in the defaultType; otherwise, it means that we
        // have never `touched` the element during the analysis. Note that `isTouched` does not point on it,
        // because there might be an aliasing between this addr and an addr of some other object, that we really
        // touched, e.g., the addr of `this` object. In such case we can remove null to construct UtNullModel later.
        if (numDimensions != defaultType.numDimensions) {
            return null
        }

        require(numDimensions == defaultType.numDimensions)

        // if we have a RefType, but not an instance of java.lang.Object, or an java.lang.Object with zero dimension
        if (defaultType is RefType) {
            val inheritors = typeResolver.findOrConstructInheritorsIncludingTypes(defaultType)
            return evaluatedType.takeIf { evaluatedType in inheritors }
                ?: fallbackToDefaultTypeIfPossible(evaluatedType, defaultType, isTouched)
        }

        defaultType as ArrayType

        return constructArrayTypeOrNull(evaluatedType, defaultType, numDimensions)
            ?: fallbackToDefaultTypeIfPossible(evaluatedType, defaultType, isTouched)
    }

    private fun constructArrayTypeOrNull(evaluatedType: Type, defaultType: ArrayType, numDimensions: Int): ArrayType? {
        if (numDimensions <= 0) return null

        val actualType = evaluatedType.makeArrayType(numDimensions)
        val actualBaseType = actualType.baseType
        val defaultBaseType = defaultType.baseType
        val defaultNumDimensions = defaultType.numDimensions

        if (actualType == defaultType) return actualType

        // i.e., if defaultType is Object[][], the actualType must be at least primType[][][]
        if (actualBaseType is PrimType && defaultBaseType.isJavaLangObject() && numDimensions > defaultNumDimensions) {
            return actualType
        }

        // i.e., if defaultType is Object[][], the actualType must be at least RefType[][]
        if (actualBaseType is RefType && defaultBaseType.isJavaLangObject() && numDimensions >= defaultNumDimensions) {
            return actualType
        }

        if (actualBaseType is RefType && defaultBaseType is RefType) {
            val inheritors = typeResolver.findOrConstructInheritorsIncludingTypes(defaultBaseType)
            // if actualBaseType in inheritors, actualType and defaultType must have the same numDimensions
            if (actualBaseType in inheritors && numDimensions == defaultNumDimensions) return actualType
        }

        return null
    }

    /**
     * Tries to determine whether it is possible to use [defaultType] instead of [actualType] or not.
     */
    private fun fallbackToDefaultTypeIfPossible(actualType: Type, defaultType: Type, isTouched: Boolean): Type? {
        // If the object hasn't been touched during the analysis, return null, because the object could be
        // replaced with null model.
        if (!isTouched) return null

        val defaultBaseType = defaultType.baseType

        // It might be confusing we do we return null instead of default type here for the touched element.
        // The answer is because sometimes we may have a real object with different type as an element here.
        // I.e. we have int[][]. In the z3 memory it is an infinite array represented by const model and stores.
        // Let's assume we know that the array has only one element. It means that solver can do whatever it wants
        // with every other element but the first one. In such cases sometimes it sets as const model (or even store
        // outside the array's length) existing objects (that has been touched during the execution) with a wrong
        // (for the array) type. Because of such cases we have to return null as a sign that construction failed.
        // If we return defaultType, it will mean that it might try to put model with an inappropriate type
        // as const or store model.
        if (defaultBaseType is PrimType) return null

        val actualBaseType = actualType.baseType

        require(actualBaseType is RefType) { "Expected RefType, but $actualBaseType found" }
        require(defaultBaseType is RefType) { "Expected RefType, but $defaultBaseType found" }

        val ancestors = typeResolver.findOrConstructAncestorsIncludingTypes(defaultBaseType)

        // This is intended to fix a specific problem. We have code:
        // ColoredPoint foo(Point[] array) {
        //     array[0].x = 5;
        //     return (ColoredPoint[]) array;
        // }
        // Since we don't have a way to connect types of the array and the elements within it, there might be situation
        // when the array is ColoredPoint[], but the first element of it got type Point from the solver.
        // In such case here we'll have ColoredPoint as defaultType and Point as actualType. It is obvious from the example
        // that we can construct ColoredPoint instance instead of it with randomly filled colored-specific fields.
        return defaultType.takeIf { actualBaseType in ancestors && actualType.numDimensions == defaultType.numDimensions }
    }

    /**
     * Constructs array model.
     */
    private fun constructArrayModel(instance: ArrayValue): UtModel {
        val concreteAddr = holder.concreteAddr(instance.addr)
        if (concreteAddr == SYMBOLIC_NULL_ADDR) {
            return UtNullModel(instance.type.id)
        }

        val evaluatedType = workaround(HACK) { memory.findTypeForArrayOrNull(instance.addr) ?: instance.type }
        val evaluatedBaseType = evaluatedType.baseType

        val chunkId = typeRegistry.arrayChunkId(evaluatedType)
        val descriptor = MemoryChunkDescriptor(chunkId, evaluatedType, evaluatedType.elementType)
        val array = findArray(descriptor, state)

        val oneDimensionalArrayChunkId = typeRegistry.arrayChunkId(evaluatedBaseType.arrayType)
        val oneDimensionalArrayDescriptor = MemoryChunkDescriptor(
            oneDimensionalArrayChunkId,
            evaluatedBaseType.arrayType,
            evaluatedBaseType
        )
        val oneDimensionalArray = findArray(oneDimensionalArrayDescriptor, state)

        return constructArrayModel(
            evaluatedType,
            concreteAddr,
            ArrayExtractionDetails(array, oneDimensionalArray),
        )
    }

    /**
     * Constructs array model from Z3 model by extracting constant value and stores.
     */
    private fun constructArrayModel(
        actualType: ArrayType,
        concreteAddr: Address,
        details: ArrayExtractionDetails,
    ): UtModel {
        if (concreteAddr == SYMBOLIC_NULL_ADDR) {
            return UtNullModel(actualType.id)
        }

        val addr = UtAddrExpression(concreteAddr)
        val lengthVar = memory.findArrayLength(addr)
        val length = max(min(holder.eval(lengthVar.expr).intValue(), softMaxArraySize), MIN_ARRAY_SIZE)

        val expression = if (actualType.numDimensions > 1) details.array else details.oneDimensionalArray
        val nestedArray = expression.select(addr)

        val unsigned = actualType.elementType.unsigned

        // TODO remove const model from api
        // It is important to notice, that, in fact, we no longer use const model, since we do not
        // evaluate `z3 array model` anymore. It happened because of the `UtArraySetRange`, that is not
        // interpreted in z3. But we can evaluate select to such array, and that is what we do below.
        val constValue = holder.eval((nestedArray.sort as UtArraySort).itemSort.defaultValue).value(unsigned)
        val stores = (0 until length).associateWith {
            holder.eval(nestedArray.select(mkInt(it))).value(unsigned)
        }
        val descriptor = ArrayDescriptor(constValue, stores)

        val arrayModel = descriptor.toModel(concreteAddr, actualType, length, details)
        addConstructedModel(concreteAddr, arrayModel)

        return arrayModel
    }

    private fun ArrayDescriptor.toModel(
        addr: Address,
        arrayType: ArrayType,
        length: Int,
        details: ArrayExtractionDetails
    ): UtArrayModel {
        val classId = arrayType.id
        return when (val elementType: Type = arrayType.elementType) {
            is PrimType -> {
                // array of primitives
                val constModel = UtPrimitiveModel(this.constValue)
                val modelStores =
                    this.stores.mapValuesTo(mutableMapOf<Int, UtModel>()) { (_, value) ->
                        UtPrimitiveModel(value)
                    }
                UtArrayModel(addr, classId, length, constModel, modelStores)
            }
            is ArrayType -> {
                val constModel = arrayOfArraysElementModel(constValue as Address, elementType, details)
                val modelStores = stores.mapValuesTo(mutableMapOf()) { (_, addr) ->
                    arrayOfArraysElementModel(addr as Address, elementType, details)
                }
                UtArrayModel(addr, classId, length, constModel, modelStores)
            }
            is RefType -> {
                val constModel = arrayOfObjectsElementModel(constValue as Address, elementType)
                val modelStores = stores.mapValuesTo(mutableMapOf()) { (_, storeAddr) ->
                    arrayOfObjectsElementModel(storeAddr as Address, elementType)
                }
                UtArrayModel(addr, classId, length, constModel, modelStores)
            }
            else -> error("Unknown elementType $elementType for $arrayType")
        }
    }

    /**
     * Constructs model for "array of arrays" element.
     *
     * Uses [constructTypeOrNull] to evaluate possible element type.
     */
    private fun arrayOfArraysElementModel(
        addr: Address,
        elementType: ArrayType,
        details: ArrayExtractionDetails
    ): UtModel {
        if (addr == SYMBOLIC_NULL_ADDR) {
            return UtNullModel(elementType.id)
        }

        // constructedType is null if a value with the type, calculated by the solver,
        // cannot be stored in the cell. That means we didn't touch that element of the array
        // during the analysis. Therefore, we can put default value in it.
        val constructedType = holder.constructTypeOrNull(UtAddrExpression(addr), elementType)

        return if (constructedType == null) {
            UtNullModel(elementType.id)
        } else {
            constructedModels.getOrPut(addr) {
                constructArrayModel(constructedType as ArrayType, addr, details)
            }
        }
    }

    /**
     * Constructs model for "array of objects" element.
     *
     * Uses [constructTypeOrNull] to evaluate possible element type.
     */
    private fun arrayOfObjectsElementModel(concreteAddr: Address, defaultType: RefType): UtModel {
        if (concreteAddr == SYMBOLIC_NULL_ADDR) {
            return UtNullModel(defaultType.id)
        }

        val addr = UtAddrExpression(concreteAddr)

        if (concreteAddr in mockInfos) {
            return resolveMock(concreteAddr)
        }

        val constructedType = holder.constructTypeOrNull(addr, defaultType) ?: return UtNullModel(defaultType.id)

        if (defaultType.isJavaLangObject() && constructedType is ArrayType) {
            return constructArrayModel(ArrayValue(TypeStorage(constructedType), addr))
        } else {
            val concreteType = typeResolver.findAnyConcreteInheritorIncludingOrDefault(
                constructedType as RefType,
                defaultType
                // need it for mocked objects in array. Sometimes we have const value without mock,
                // but all the objects inside the array will be mocked.
            ) ?: return UtNullModel(defaultType.sootClass.id)

            resolveWrappersAndOverriddenTypesOrNull(concreteType, addr, defaultType)?.let { return it }

            return constructModel(addr, defaultType, concreteType)
        }
    }
}

// TODO: reuse something powerful from Soot or whatever to recreate _all_ names from signatures, including [[Z
internal fun String.toClassName() = this.removeSurrounding("L", ";").replace('/', '.')

/**
 * Represents array in Z3 const value style. Contains constant value to fill whole array and stores to
 * overwrite constant value with something different.
 *
 * Note: array of objects/arrays contains addresses instead of objects/arrays and requires additional resolve.
 *
 * @see org.utbot.engine.Resolver.toModel
 */
internal data class ArrayDescriptor(val constValue: Any, val stores: Map<Int, Any>)

private data class FieldWithChunkId(val field: SootField, val chunkId: ChunkId)

private data class ArrayExtractionDetails(
    val array: UtArrayExpressionBase,
    val oneDimensionalArray: UtArrayExpressionBase
)

internal val nullObjectAddr = UtAddrExpression(mkInt(SYMBOLIC_NULL_ADDR))


fun SymbolicValue.isNullObject() =
    when (this) {
        is ReferenceValue -> addr == nullObjectAddr
        is PrimitiveValue -> false
    }

fun Any?.primitiveToLiteral() = when (this) {
    null -> nullObjectAddr
    is Byte -> mkByte(this)
    is Short -> mkShort(this)
    is Char -> mkChar(this)
    is Int -> mkInt(this)
    is Long -> mkLong(this)
    is Float -> mkFloat(this)
    is Double -> mkDouble(this)
    is Boolean -> mkBool(this)
    else -> error("Unknown class: ${this::class} to convert to Literal")
}

fun Any?.primitiveToSymbolic() = when (this) {
    null -> nullObjectAddr.toIntValue()
    is Byte -> this.toPrimitiveValue()
    is Short -> this.toPrimitiveValue()
    is Char -> this.toPrimitiveValue()
    is Int -> this.toPrimitiveValue()
    is Long -> this.toPrimitiveValue()
    is Float -> this.toPrimitiveValue()
    is Double -> this.toPrimitiveValue()
    is Boolean -> this.toPrimitiveValue()
    is Unit -> voidValue
    else -> error("Unknown class: ${this::class} to convert to PrimitiveValue")
}

val typesOfObjectsToRecreate = listOf(
    "java.lang.CharacterDataLatin1",
    "java.lang.CharacterData00",
    "[Ljava.lang.StackTraceElement",
    "sun.java2d.cmm.lcms.LcmsServiceProvider",
    PrintStream::class.qualifiedName,
    AccessControlContext::class.qualifiedName,
    ICC_ProfileRGB::class.qualifiedName,
    AtomicInteger::class.qualifiedName
)

/**
 * Transforms the concrete [value] into a symbolic representation.
 *
 * [sootType] is required for two situations:
 * * we have to determine, which null values must be constructed;
 * * we must distinguish primitives and wrappers, but because of kotlin types we cannot do it without the [sootType];
 */
fun Traverser.toMethodResult(value: Any?, sootType: Type): MethodResult {
    if (sootType is PrimType) return MethodResult(value.primitiveToSymbolic())

    return when (value) {
        null -> asMethodResult {
            if (sootType is RefType) {
                createObject(nullObjectAddr, sootType, useConcreteType = true)
            } else {
                createArray(nullObjectAddr, sootType as ArrayType, useConcreteType = true)
            }
        }
        is String -> {
            val newAddr = findNewAddr()
            MethodResult(objectValue(STRING_TYPE, newAddr, StringWrapper()))
        }
        is ByteArray -> arrayToMethodResult(value.size, ByteType.v()) { mkByte(value[it]) }
        is ShortArray -> arrayToMethodResult(value.size, ShortType.v()) { mkShort(value[it]) }
        is CharArray -> {
            // why only char[] has this constraint?
            arrayToMethodResult(min(value.size, softMaxArraySize), CharType.v()) { mkChar(value[it]) }
        }
        is IntArray -> arrayToMethodResult(value.size, IntType.v()) { mkInt(value[it]) }
        is LongArray -> arrayToMethodResult(value.size, LongType.v()) { mkLong(value[it]) }
        is FloatArray -> arrayToMethodResult(value.size, FloatType.v()) { mkFloat(value[it]) }
        is DoubleArray -> arrayToMethodResult(value.size, DoubleType.v()) { mkDouble(value[it]) }
        is BooleanArray -> arrayToMethodResult(value.size, BooleanType.v()) { mkBool(value[it]) }

        is Class<*> -> typeRegistry.createClassRef(Scene.v().getType(value.name))
        is Array<*> -> {
            // if we have an array of objects, we have to fill corresponding array
            // with values -- zeroes for null values and unbounded symbolic addresses otherwise.
            // TODO It should be replaced with objects construction along with
            // TODO JIRA:1579
            val elementType = (sootType as? ArrayType)?.elementType ?: OBJECT_TYPE

            arrayToMethodResult(value.size, elementType) {
                if (value[it] == null) return@arrayToMethodResult nullObjectAddr

                val addr = UtAddrExpression(mkBVConst("staticVariable${value.hashCode()}_$it", UtInt32Sort))

                val createdElement = if (elementType is RefType) {
                    val className = value[it]!!.javaClass.id.name
                    createObject(addr, Scene.v().getRefType(className), useConcreteType = true)
                } else {
                    require(elementType is ArrayType)
                    // We cannot use concrete types since we do not receive
                    // type information from the runtime in oppose to the case above.
                    createArray(addr, elementType, useConcreteType = false)
                }

                createdElement.addr
            }
        }
        else -> {
            workaround(RUN_CONCRETE) {
                val className = value.javaClass.id.name
                val superclassName = value.javaClass.superclass.name

                val refTypeName = when {
                    // hardcoded string is used cause class is not public
                    className in typesOfObjectsToRecreate -> className
                    superclassName == PrintStream::class.qualifiedName -> superclassName
                    // we want to generate an unbounded symbolic variable for every unknown class as well
                    else -> workaround(MAKE_SYMBOLIC) { className }
                }

                return asMethodResult {
                    val addr = UtAddrExpression(mkBVConst("staticVariable${value.hashCode()}", UtInt32Sort))
                    createObject(addr, Scene.v().getRefType(refTypeName), useConcreteType = true)
                }
            }
        }
    }
}

private fun Traverser.arrayToMethodResult(
    size: Int,
    elementType: Type,
    takeElement: (Int) -> UtExpression
): MethodResult {
    val updatedSize = min(size, HARD_MAX_ARRAY_SIZE)
    val newAddr = findNewAddr()
    val length = memory.findArrayLength(newAddr)

    val type = Scene.v().getType(ArrayType.v(elementType, 1).toString()) as ArrayType
    val chunkId = typeRegistry.arrayChunkId(type)

    val typeStorage = typeResolver.constructTypeStorage(type, useConcreteType = true)
    val constraints = setOf(
        Eq(length, updatedSize),
        typeRegistry.typeConstraint(newAddr, typeStorage).all()
    )

    val descriptor = MemoryChunkDescriptor(chunkId, type, elementType)
    val array: UtExpression = memory.findArray(descriptor).select(newAddr)

    val updatedArray = (0 until updatedSize).fold(array) { old, index ->
        old.store(mkInt(index), takeElement(index))
    }

    val memoryUpdate = MemoryUpdate(
        stores = persistentListOf(simplifiedNamedStore(descriptor, newAddr, updatedArray)),
        touchedChunkDescriptors = persistentSetOf(descriptor)
    )
    return MethodResult(
        ArrayValue(typeStorage, newAddr),
        constraints.asHardConstraint(),
        memoryUpdates = memoryUpdate
    )
}

fun Traverser.constructEnumStaticFieldResult(
    fieldName: String,
    fieldType: Type,
    declaringClass: SootClass,
    enumConstantSymbolicResultsByName: Map<String, MethodResult>,
    staticFieldConcreteValue: Any?,
    enumConstantSymbolicValues: List<ObjectValue>
): MethodResult =
    if (isEnumValuesFieldName(fieldName)) {
        // special case to fill $VALUES array with already created symbolic values for enum constants runtime values
        arrayToMethodResult(enumConstantSymbolicValues.size, declaringClass.type) { i ->
            enumConstantSymbolicValues[i].addr
        }
    } else {
        if (fieldName in enumConstantSymbolicResultsByName) {
            // it is field to store enum constant so we use already created symbolic value from runtime enum constant
            enumConstantSymbolicResultsByName.getValue(fieldName)
        } else {
            // otherwise, it is a common static field,
            // and we have to create new symbolic value for it using its concrete value
            toMethodResult(staticFieldConcreteValue, fieldType)
        }
    }

private val Type.unsigned: Boolean
    get() = this is CharType

data class MockInfoEnriched(
    val mockInfo: UtMockInfo,
    val executables: Map<ExecutableId, List<MockExecutableInstance>> = emptyMap()
)

/**
 * Merges executables for the list where every element has the same MockInfo.
 * Used in the situations where two mock objects has the same address and several different executables.
 */
private fun List<MockInfoEnriched>.mergeExecutables(): MockInfoEnriched {
    val executables = this.map { it.executables }
    val methodToExecutables = mutableMapOf<ExecutableId, List<MockExecutableInstance>>()

    executables.forEach { executableIdToExecutables ->
        executableIdToExecutables.toList().forEach { (executableId, executables) ->
            methodToExecutables.merge(executableId, executables) { old, new -> old + new }
        }
    }

    methodToExecutables.entries.forEach { (executableId, executables) ->
        methodToExecutables[executableId] = executables.sortedBy { it.id }
    }

    val mockInfoEnriched = first() // all MockInfoEnriched have the same mockInfo, so we can replace only executables
    return mockInfoEnriched.copy(executables = methodToExecutables)
}

data class ResolvedModels(val parameters: List<UtModel>, val statics: Map<FieldId, UtModel>)