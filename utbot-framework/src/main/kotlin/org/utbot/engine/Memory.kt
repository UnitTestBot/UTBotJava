package org.utbot.engine

import com.google.common.collect.HashBiMap
import org.utbot.common.WorkaroundReason.MAKE_SYMBOLIC
import org.utbot.common.workaround
import org.utbot.engine.MemoryState.CURRENT
import org.utbot.engine.MemoryState.INITIAL
import org.utbot.engine.MemoryState.STATIC_INITIAL
import org.utbot.engine.overrides.strings.UtNativeString
import org.utbot.engine.pc.RewritingVisitor
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtAddrSort
import org.utbot.engine.pc.UtArrayExpressionBase
import org.utbot.engine.pc.UtArraySelectExpression
import org.utbot.engine.pc.UtArraySort
import org.utbot.engine.pc.UtBoolExpression
import org.utbot.engine.pc.UtBoolSort
import org.utbot.engine.pc.UtConstArrayExpression
import org.utbot.engine.pc.UtEqGenericTypeParametersExpression
import org.utbot.engine.pc.UtExpression
import org.utbot.engine.pc.UtFalse
import org.utbot.engine.pc.UtGenericExpression
import org.utbot.engine.pc.UtInt32Sort
import org.utbot.engine.pc.UtIntSort
import org.utbot.engine.pc.UtIsExpression
import org.utbot.engine.pc.UtIsGenericTypeExpression
import org.utbot.engine.pc.UtMkArrayExpression
import org.utbot.engine.pc.UtMkTermArrayExpression
import org.utbot.engine.pc.UtSeqSort
import org.utbot.engine.pc.UtSort
import org.utbot.engine.pc.UtTrue
import org.utbot.engine.pc.mkAnd
import org.utbot.engine.pc.mkArrayConst
import org.utbot.engine.pc.mkArrayWithConst
import org.utbot.engine.pc.mkEq
import org.utbot.engine.pc.mkInt
import org.utbot.engine.pc.mkNot
import org.utbot.engine.pc.mkOr
import org.utbot.engine.pc.select
import org.utbot.engine.pc.store
import org.utbot.engine.pc.toSort
import org.utbot.engine.symbolic.asHardConstraint
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import org.utbot.framework.plugin.api.classId
import soot.ArrayType
import soot.BooleanType
import soot.ByteType
import soot.CharType
import soot.DoubleType
import soot.FloatType
import soot.IntType
import soot.LongType
import soot.RefLikeType
import soot.RefType
import soot.Scene
import soot.ShortType
import soot.SootClass
import soot.SootField
import soot.SootMethod
import soot.Type
import soot.tagkit.AnnotationClassElem


/**
 * Represents a memory associated with a certain method call. For now consists only of local variables mapping.
 * TODO: think on other fields later
 *
 * @param [locals] represents a mapping from [LocalVariable]s of a specific method call to [SymbolicValue]s.
 */
data class LocalVariableMemory(
    private val locals: PersistentMap<LocalVariable, SymbolicValue> = persistentHashMapOf()
) {
    fun memoryForNestedMethod(): LocalVariableMemory = this.copy(locals = persistentHashMapOf())

    fun update(update: LocalMemoryUpdate): LocalVariableMemory = this.copy(locals = locals.update(update.locals))

    /**
     * Returns local variable value.
     */
    fun local(variable: LocalVariable): SymbolicValue? = locals[variable]

    val localValues: Set<SymbolicValue>
        get() = locals.values.toSet()
}

/**
 * Local memory implementation based on arrays.
 *
 * Contains initial and current versions of arrays. Also collects memory updates (stores) and can return them.
 * Updates can be reset to collect them for particular piece of code.
 *
 * [touchedAddresses] is a field used to determine whether some address has been touched during the analysis or not.
 * It is important during the resolving in [Resolver.constructTypeOrNull]. At the beginning it contains only false
 * values, therefore at the end of the execution true will be only in cells corresponding to the touched addresses.
 *
 * Fields used for statics:
 * * [staticFieldsStates] is a map containing initial and current state for every touched static field;
 * * [meaningfulStaticFields] is a set containing id for the field that has been touched outside <clinit> blocks.
 *
 * Note: [staticInitial] contains mapping from [FieldId] to the memory state at the moment of the field initialization.
 *
 * @see memoryForNestedMethod
 * @see FieldStates
 */
data class Memory( // TODO: split purely symbolic memory and information about symbolic variables mapping
    private val initial: PersistentMap<ChunkId, UtArrayExpressionBase> = persistentHashMapOf(),
    private val current: PersistentMap<ChunkId, UtArrayExpressionBase> = persistentHashMapOf(),
    private val staticInitial: PersistentMap<FieldId, PersistentMap<ChunkId, UtArrayExpressionBase>> = persistentHashMapOf(),
    private val concrete: PersistentMap<UtAddrExpression, Concrete> = persistentHashMapOf(),
    private val mockInfos: PersistentList<MockInfoEnriched> = persistentListOf(),
    private val staticInstanceStorage: PersistentMap<ClassId, ObjectValue> = persistentHashMapOf(),
    private val initializedStaticFields: PersistentSet<FieldId> = persistentHashSetOf(),
    private val staticFieldsStates: PersistentMap<FieldId, FieldStates> = persistentHashMapOf(),
    private val meaningfulStaticFields: PersistentSet<FieldId> = persistentHashSetOf(),
    private val addrToArrayType: PersistentMap<UtAddrExpression, ArrayType> = persistentHashMapOf(),
    private val addrToMockInfo: PersistentMap<UtAddrExpression, UtMockInfo> = persistentHashMapOf(),
    private val updates: MemoryUpdate = MemoryUpdate(), // TODO: refactor this later. Now we use it only for statics substitution
    private val visitedValues: UtArrayExpressionBase = UtConstArrayExpression(
        mkInt(0),
        UtArraySort(UtAddrSort, UtIntSort)
    ),
    private val touchedAddresses: UtArrayExpressionBase = UtConstArrayExpression(
        UtFalse,
        UtArraySort(UtAddrSort, UtBoolSort)
    ),
    private val instanceFieldReadOperations: PersistentSet<InstanceFieldReadOperation> = persistentHashSetOf(),

    /**
     * Storage for addresses that we speculatively consider non-nullable (e.g., final fields of system classes).
     * See [org.utbot.engine.UtBotSymbolicEngine.createFieldOrMock] for usage,
     * and [docs/SpeculativeFieldNonNullability.md] for details.
     */
    private val speculativelyNotNullAddresses: UtArrayExpressionBase = UtConstArrayExpression(
        UtFalse,
        UtArraySort(UtAddrSort, UtBoolSort)
    ),
    private val symbolicEnumValues: PersistentList<ObjectValue> = persistentListOf()
) {
    val chunkIds: Set<ChunkId>
        get() = initial.keys

    fun mockInfoByAddr(addr: UtAddrExpression): UtMockInfo? = addrToMockInfo[addr]

    fun mocks(): List<MockInfoEnriched> = mockInfos

    fun staticFields(): Map<FieldId, FieldStates> = staticFieldsStates.filterKeys { it in meaningfulStaticFields }

    /**
     * Construct the mapping from addresses to sets of fields whose values are read during the code execution
     * and therefore should be initialized in a constructed model.
     *
     * @param transformAddress the function to transform the symbolic object addresses (e.g., to translate
     * symbolic addresses into concrete addresses).
     */
    fun <TAddress> initializedFields(transformAddress: (UtAddrExpression) -> TAddress): Map<TAddress, Set<FieldId>> =
        instanceFieldReadOperations
            .groupBy { transformAddress(it.addr) }
            .mapValues { it.value.map { read -> read.fieldId }.toSet() }

    fun isVisited(addr: UtAddrExpression): UtArraySelectExpression = visitedValues.select(addr)

    /**
     * Returns symbolic information about whether [addr] has been touched during the analysis or not.
     */
    fun isTouched(addr: UtAddrExpression): UtArraySelectExpression = touchedAddresses.select(addr)

    /**
     * Returns symbolic information about whether [addr] corresponds to a final field known to be not null.
     */
    fun isSpeculativelyNotNull(addr: UtAddrExpression): UtArraySelectExpression = speculativelyNotNullAddresses.select(addr)

    /**
     * @return ImmutableCollection of the initial values for all the arrays we touched during the execution
     */
    val initialArrays: ImmutableCollection<UtArrayExpressionBase>
        get() = initial.values

    /**
     * Finds the array by given [chunkDescriptor] and [state]. In case when [state] is [MemoryState.STATIC_INITIAL]
     * [staticFieldId] must be not null, because it means that we want to get arrays existed in the initial moment
     * for specified field (the moment of its initialization).
     *
     * Note: do not use this method directly during resolving results, use [Resolver.findArray] instead.
     * Otherwise, might occur a situation when we try to find array in [STATIC_INITIAL] state without
     * specified [staticFieldId] (i.e., during the wrappers resolving).
     *
     * @see Resolver.findArray
     */
    fun findArray(
        chunkDescriptor: MemoryChunkDescriptor,
        state: MemoryState = CURRENT,
        staticFieldId: FieldId? = null
    ): UtArrayExpressionBase =
        when (state) {
            INITIAL -> initial[chunkDescriptor.id]
            CURRENT -> current[chunkDescriptor.id]
            STATIC_INITIAL -> staticInitial[staticFieldId!!]?.get(chunkDescriptor.id)
        } ?: initialArray(chunkDescriptor)

    fun update(update: MemoryUpdate): Memory {
        var updInitial = initial
        var updCurrent = current
        update.touchedChunkDescriptors
            .filterNot { it.id in updCurrent }
            .forEach { chunk ->
                val array = initialArray(chunk)
                updInitial = updInitial.put(chunk.id, array)
                updCurrent = updCurrent.put(chunk.id, array)
            }
        // TODO: collect updates for one array
        update.stores.forEach { (descriptor, index, value) ->
            val array = updCurrent[descriptor.id] ?: initialArray(descriptor)
            val nextArray = array.store(index, value)
            updCurrent = updCurrent.put(descriptor.id, nextArray)
        }

        val initialMemoryStates = mutableMapOf<FieldId, PersistentMap<ChunkId, UtArrayExpressionBase>>()

        // sometimes we might have several updates for the same fieldId. It means that we updated it in the nested
        // calls. In this case we should care only about the first and the last one value in the updates.
        val staticFieldUpdates = update.staticFieldsUpdates
            .groupBy { it.fieldId }
            .mapValues { (_, values) -> values.first().value to values.last().value }

        val previousMemoryStates = staticFieldsStates.toMutableMap()


        /**
         * sometimes we want to change initial memory states of fields of a certain class, so we erase all the
         * information about previous states and update it with the current state. For now, this processing only takes
         * place after receiving MethodResult from [STATIC_INITIALIZER] method call at the end of
         * [Traverser.processStaticInitializer]. The value of `update.classIdToClearStatics` is equal to the
         * class for which the static initialization has performed.
         * TODO: JIRA:1610 -- refactor working with statics later
         */
        update.classIdToClearStatics?.let { classId ->
            Scene.v().getSootClass(classId.name).fields.forEach { sootField ->
                previousMemoryStates.remove(sootField.fieldId)
            }
        }

        val updatedStaticFields = staticFieldUpdates
            .map { (fieldId, values) ->
                val (initialValue, currentValue) = values

                val previousValues = previousMemoryStates[fieldId]
                var updatedValue = previousValues?.copy(stateAfter = currentValue)

                if (updatedValue == null) {
                    require(fieldId !in initialMemoryStates)
                    initialMemoryStates[fieldId] = updCurrent
                    updatedValue = FieldStates(stateBefore = initialValue, stateAfter = currentValue)
                }

                fieldId to updatedValue
            }
            .toMap()

        val updVisitedValues = update.visitedValues.fold(visitedValues) { acc, addr ->
            acc.store(addr, mkInt(1))
        }

        val updTouchedAddresses = update.touchedAddresses.fold(touchedAddresses) { acc, addr ->
            acc.store(addr, UtTrue)
        }

        val updSpeculativelyNotNullAddresses = update.speculativelyNotNullAddresses.fold(speculativelyNotNullAddresses) { acc, addr ->
            acc.store(addr, UtTrue)
        }

        return this.copy(
            initial = updInitial,
            current = updCurrent,
            staticInitial = staticInitial.putAll(initialMemoryStates),
            concrete = concrete.putAll(update.concrete),
            mockInfos = mockInfos.mergeWithUpdate(update.mockInfos),
            staticInstanceStorage = staticInstanceStorage.putAll(update.staticInstanceStorage),
            initializedStaticFields = initializedStaticFields.addAll(update.initializedStaticFields),
            staticFieldsStates = previousMemoryStates.toPersistentMap().putAll(updatedStaticFields),
            meaningfulStaticFields = meaningfulStaticFields.addAll(update.meaningfulStaticFields),
            addrToArrayType = addrToArrayType.putAll(update.addrToArrayType),
            addrToMockInfo = addrToMockInfo.putAll(update.addrToMockInfo),
            updates = updates + update,
            visitedValues = updVisitedValues,
            touchedAddresses = updTouchedAddresses,
            instanceFieldReadOperations = instanceFieldReadOperations.addAll(update.instanceFieldReads),
            speculativelyNotNullAddresses = updSpeculativelyNotNullAddresses,
            symbolicEnumValues = symbolicEnumValues.addAll(update.symbolicEnumValues)
        )
    }

    /**
     * Returns copy of memory without local variables and updates.
     * Execution can continue to collect updates for particular piece of code.
     */
    fun memoryForNestedMethod(): Memory = this.copy(updates = MemoryUpdate())

    /**
     * Returns copy of queued [updates] which consists only of updates of static fields.
     * This is necessary for substituting unbounded symbolic variables into the static fields.
     */
    fun queuedStaticMemoryUpdates(): MemoryUpdate = MemoryUpdate(
        staticInstanceStorage = updates.staticInstanceStorage,
        staticFieldsUpdates = updates.staticFieldsUpdates
    )

    /**
     * Creates UtArraySelect for array length with particular array address. Addresses are unique for all objects.
     * No need to track updates on arraysLength array, cause we use selects only with unique ids.
     */
    fun findArrayLength(addr: UtAddrExpression) = arraysLength.select(addr).toIntValue()

    private val arraysLength: UtMkArrayExpression by lazy {
        mkArrayConst("arraysLength", UtAddrSort, UtInt32Sort)
    }

    /**
     * Makes the lengths for all the arrays in the program equal to zero by default
     */
    fun softZeroArraysLengths() = UtMkTermArrayExpression(arraysLength)

    /**
     * Returns concrete value for address.
     *
     * Note: for initial state returns null.
     */
    fun takeConcrete(addr: UtAddrExpression, state: MemoryState): Concrete? =
        when (state) {
            INITIAL, STATIC_INITIAL -> null // no values in the beginning
            CURRENT -> concrete[addr]
        }

    fun isInitialized(id: ClassId): Boolean = id in staticInstanceStorage

    fun isInitialized(fieldId: FieldId): Boolean = fieldId in initializedStaticFields

    fun findStaticInstanceOrNull(id: ClassId): ObjectValue? = staticInstanceStorage[id]

    fun findTypeForArrayOrNull(addr: UtAddrExpression): ArrayType? = addrToArrayType[addr]

    fun getSymbolicEnumValues(classId: ClassId): List<ObjectValue> =
        symbolicEnumValues.filter { it.type.classId == classId }
}

/**
 * Types/classes registry.
 *
 * Registers and keeps two mappings:
 * - Type <-> unique type id (int)
 * - Object address -> to type id
 */
class TypeRegistry {
    init {
        // initializes type storage for OBJECT_TYPE from current scene
        objectTypeStorage = TypeStorage(OBJECT_TYPE, Scene.v().classes.mapTo(mutableSetOf()) { it.type })
    }

    private val typeIdBiMap = HashBiMap.create<Type, Int>()

    // A cache for strings representing bit-vectors for some collection of types.
    private val typesToBitVecString = mutableMapOf<Collection<Type>, String>()
    private val typeToRating = mutableMapOf<RefType, Int>()
    private val typeToInheritorsTypes = mutableMapOf<RefType, Set<RefType>>()
    private val typeToAncestorsTypes = mutableMapOf<RefType, Set<RefType>>()

    /**
     * Contains types storages for type parameters of object by its address.
     */
    private val genericTypeStorageByAddr = mutableMapOf<UtAddrExpression, List<TypeStorage>>()

    // A BiMap containing bijection from every type to an address of the object
    // presenting its classRef and vise versa
    private val classRefBiMap = HashBiMap.create<Type, UtAddrExpression>()

    /**
     * Contains mapping from a class to the class containing substitutions for its methods.
     */
    private val targetToSubstitution: Map<SootClass, SootClass> by lazy {
        val classesWithTargets = Scene.v().classes.mapNotNull { clazz ->
            val annotation = clazz.findMockAnnotationOrNull
                ?.elems
                ?.singleOrNull { it.name == "target" } as? AnnotationClassElem

            val classNameFromSignature = classBytecodeSignatureToClassNameOrNull(annotation?.desc)

            if (classNameFromSignature == null) {
                null
            } else {
                val target = Scene.v().getSootClass(classNameFromSignature)
                target to clazz
            }
        }
        classesWithTargets.toMap()
    }

    /**
     * Contains mapping from a class with substitutions of the methods of the target class to the target class itself.
     */
    private val substitutionToTarget: Map<SootClass, SootClass> by lazy {
        targetToSubstitution.entries.associate { (k, v) -> v to k }
    }

    private val typeToFields = mutableMapOf<RefType, List<SootField>>()

    /**
     * An array containing information about whether the object with particular addr could throw a [ClassCastException].
     *
     * Note: all objects can throw it by default.
     * @see disableCastClassExceptionCheck
     */
    private var isClassCastExceptionAllowed: UtArrayExpressionBase =
        mkArrayWithConst(UtArraySort(UtAddrSort, UtBoolSort), UtTrue)


    /**
     * Contains information about types for ReferenceValues.
     * An element on some position k contains information about type for an object with address == k
     * Each element in addrToTypeId is in range [1..numberOfTypes]
     */
    private val addrToTypeId: UtArrayExpressionBase by lazy {
        mkArrayConst(
            "addrToTypeId",
            UtAddrSort,
            UtInt32Sort
        )
    }

    private val genericAddrToTypeArrays = mutableMapOf<Int, UtArrayExpressionBase>()

    private fun genericAddrToType(i: Int) = genericAddrToTypeArrays.getOrPut(i) {
        mkArrayConst(
            "genericAddrToTypeId_$i",
            UtAddrSort,
            UtInt32Sort
        )
    }

    /**
     * Contains information about number of dimensions for ReferenceValues.
     */
    private val addrToNumDimensions: UtArrayExpressionBase by lazy {
        mkArrayConst(
            "addrToNumDimensions",
            UtAddrSort,
            UtInt32Sort
        )
    }

    private val genericAddrToNumDimensionsArrays = mutableMapOf<Int, UtArrayExpressionBase>()

    private fun genericAddrToNumDimensions(i: Int) = genericAddrToNumDimensionsArrays.getOrPut(i) {
        mkArrayConst(
            "genericAddrToNumDimensions_$i",
            UtAddrSort,
            UtInt32Sort
        )
    }

    /**
     * Contains information about whether the object with some addr is a mock or not.
     */
    private val isMockArray: UtArrayExpressionBase by lazy {
        mkArrayConst(
            "isMock",
            UtAddrSort,
            UtBoolSort
        )
    }

    /**
     * Takes information about whether the object with [addr] is mock or not.
     *
     * @see isMockArray
     */
    fun isMock(addr: UtAddrExpression) = isMockArray.select(addr)

    /**
     * Makes the numbers of dimensions for every object in the program equal to zero by default
     */
    fun softZeroNumDimensions() = UtMkTermArrayExpression(addrToNumDimensions)

    /**
     * addrToTypeId is created as const array of the emptyType. If such type occurs anywhere in the program, it means
     * we haven't touched the element that this type belongs to
     * @see emptyTypeId
     */
    fun softEmptyTypes() = UtMkTermArrayExpression(addrToTypeId, mkInt(emptyTypeId))

    /**
     * Calculates type 'rating' for a particular type. Used for ordering ObjectValue's possible types.
     * The type with a higher rating is more likely than the one with a lower rating.
     */
    fun findRating(type: RefType) = typeToRating.getOrPut(type) {
        var finalCost = 0

        val sootClass = type.sootClass

        // TODO: let's have "preferred types"
        if (sootClass.name == "java.util.ArrayList") finalCost += 4096
        if (sootClass.name == "java.util.LinkedList") finalCost += 2048
        if (sootClass.name == "java.util.HashMap") finalCost += 4096
        if (sootClass.name == "java.util.TreeMap") finalCost += 2048
        if (sootClass.name == "java.util.HashSet") finalCost += 2048
        if (sootClass.name == "java.lang.Integer") finalCost += 8192
        if (sootClass.name == "java.lang.Character") finalCost += 8192
        if (sootClass.name == "java.lang.Double") finalCost += 8192
        if (sootClass.name == "java.lang.Long") finalCost += 8192

        if (sootClass.packageName.startsWith("java.lang")) finalCost += 1024

        if (sootClass.packageName.startsWith("java.util")) finalCost += 512

        if (sootClass.packageName.startsWith("java")) finalCost += 128

        if (sootClass.isPublic) finalCost += 16

        if (sootClass.isPrivate) finalCost += -16

        if ("blocking" in sootClass.name.toLowerCase()) finalCost -= 32

        if (sootClass.type.isJavaLangObject()) finalCost += -32

        if (sootClass.isAnonymous) finalCost -= 128

        if (sootClass.name.contains("$")) finalCost += -4096

        if (sootClass.type.sootClass.isInappropriate) finalCost += -8192

        finalCost
    }

    private val classRefCounter = AtomicInteger(classRefAddrsInitialValue)
    private fun nextClassRefAddr() = UtAddrExpression(classRefCounter.getAndIncrement())

    private val symbolicReturnNameCounter = AtomicLong(symbolicReturnNameCounterInitialValue)
    fun findNewSymbolicReturnValueName() =
        workaround(MAKE_SYMBOLIC) { "symbolicReturnValue\$${symbolicReturnNameCounter.incrementAndGet()}" }

    private val typeCounter = AtomicInteger(typeCounterInitialValue)
    private fun nextTypeId() = typeCounter.getAndIncrement()

    /**
     * Returns unique typeId for the given type
     */
    fun findTypeId(type: Type): Int = typeIdBiMap.getOrPut(type) { nextTypeId() }

    /**
     * Returns type for the given typeId
     *
     * @return If there is such typeId in the program, returns the corresponding type, otherwise returns null
     */
    fun typeByIdOrNull(typeId: Int): Type? = typeIdBiMap.getByValue(typeId)

    /**
     * Returns symbolic representation for a typeId corresponding to the given address
     */
    fun symTypeId(addr: UtAddrExpression) = addrToTypeId.select(addr)

    /**
     * Returns a symbolic representation for an [i]th type parameter
     * corresponding to the given address
     */
    fun genericTypeId(addr: UtAddrExpression, i: Int) = genericAddrToType(i).select(addr)

    /**
     * Returns symbolic representation for a number of dimensions corresponding to the given address
     */
    fun symNumDimensions(addr: UtAddrExpression) = addrToNumDimensions.select(addr)

    fun genericNumDimensions(addr: UtAddrExpression, i: Int) = genericAddrToNumDimensions(i).select(addr)

    /**
     * Returns a constraint stating that number of dimensions for the given address is zero
     */
    fun zeroDimensionConstraint(addr: UtAddrExpression) = mkEq(symNumDimensions(addr), mkInt(objectNumDimensions))

    /**
     * Constructs a binary bit-vector by the given types with length 'numberOfTypes'. Each position
     * corresponding to one of the typeId.
     *
     * @param types  the collection of possible type
     * @return decimal string representing the binary bit-vector
     */
    fun constructBitVecString(types: Collection<Type>) = typesToBitVecString.getOrPut(types) {
        val initialValue = BigInteger(ByteArray(numberOfTypes) { 0 })

        return types.fold(initialValue) { acc, type ->
            val typeId = if (type is ArrayType) findTypeId(type.baseType) else findTypeId(type)
            acc.setBit(typeId)
        }.toString()
    }

    /**
     * Creates class reference, i.e. Class&lt;Integer&gt;
     *
     * Note: Uses type id as an address to have the one and the same class reference for all objects of one class
     */
    fun createClassRef(baseType: Type, numDimensions: Int = 0): MethodResult {
        val addr = classRefBiMap.getOrPut(baseType) { nextClassRefAddr() }

        val objectValue = ObjectValue(TypeStorage(CLASS_REF_TYPE), addr)

        val typeConstraint = typeConstraint(addr, TypeStorage(CLASS_REF_TYPE)).all()

        val typeId = mkInt(findTypeId(baseType))
        val symNumDimensions = mkInt(numDimensions)

        val stores = persistentListOf(
            simplifiedNamedStore(CLASS_REF_TYPE_DESCRIPTOR, addr, typeId),
            simplifiedNamedStore(CLASS_REF_NUM_DIMENSIONS_DESCRIPTOR, addr, symNumDimensions)
        )

        val touchedDescriptors = persistentSetOf(CLASS_REF_TYPE_DESCRIPTOR, CLASS_REF_NUM_DIMENSIONS_DESCRIPTOR)

        val memoryUpdate = MemoryUpdate(stores = stores, touchedChunkDescriptors = touchedDescriptors)

        return MethodResult(objectValue, typeConstraint.asHardConstraint(), memoryUpdates = memoryUpdate)
    }

    /**
     * Returns a list of inheritors for the given [type], including itself.
     */
    fun findInheritorsIncludingTypes(type: RefType, defaultValue: () -> Set<RefType>) =
        typeToInheritorsTypes.getOrPut(type, defaultValue)

    /**
     * Returns a list of ancestors for the given [type], including itself.
     */
    fun findAncestorsIncludingTypes(type: RefType, defaultValue: () -> Set<RefType>) =
        typeToAncestorsTypes.getOrPut(type, defaultValue)

    fun findFields(type: RefType, defaultValue: () -> List<SootField>) =
        typeToFields.getOrPut(type, defaultValue)

    /**
     * Returns a [TypeConstraint] instance for the given [addr] and [typeStorage].
     */
    fun typeConstraint(addr: UtAddrExpression, typeStorage: TypeStorage): TypeConstraint =
        TypeConstraint(
            constructIsExpression(addr, typeStorage),
            mkEq(addr, nullObjectAddr),
            constructCorrectnessConstraint(addr, typeStorage)
        )

    private fun constructIsExpression(addr: UtAddrExpression, typeStorage: TypeStorage): UtIsExpression =
        UtIsExpression(addr, typeStorage, numberOfTypes)

    /**
     * Returns a conjunction of the constraints responsible for the type construction:
     * * typeId must be in range [[emptyTypeId]..[numberOfTypes]];
     * * numDimensions must be in range [0..[MAX_NUM_DIMENSIONS]];
     * * if the baseType for [TypeStorage.leastCommonType] is a [java.lang.Object],
     * should be added constraints for primitive arrays to prevent
     * impossible resolved types: Object[] must be at least primType[][].
     */
    private fun constructCorrectnessConstraint(addr: UtAddrExpression, typeStorage: TypeStorage): UtBoolExpression {
        val symType = symTypeId(addr)
        val symNumDimensions = symNumDimensions(addr)
        val type = typeStorage.leastCommonType

        val constraints = mutableListOf<UtBoolExpression>()

        // add constraints for typeId, it must be in 0..numberOfTypes
        constraints += Ge(symType.toIntValue(), emptyTypeId.toPrimitiveValue())
        constraints += Le(symType.toIntValue(), numberOfTypes.toPrimitiveValue())

        // add constraints for number of dimensions, it must be in 0..MAX_NUM_DIMENSIONS
        constraints += Ge(symNumDimensions.toIntValue(), 0.toPrimitiveValue())
        constraints += Le(symNumDimensions.toIntValue(), MAX_NUM_DIMENSIONS.toPrimitiveValue())

        // add constraints for object and arrays of primitives
        if (type.baseType.isJavaLangObject()) {
            primTypes.forEach {
                val typesAreEqual = mkEq(symType, mkInt(findTypeId(it)))
                val numDimensions = Gt(symNumDimensions.toIntValue(), type.numDimensions.toPrimitiveValue())
                constraints += mkOr(mkNot(typesAreEqual), numDimensions)
            }
        }

        // there are no arrays of anonymous classes
        typeStorage.possibleConcreteTypes
            .mapNotNull { (it.baseType as? RefType) }
            .filter { it.sootClass.isAnonymous }
            .forEach {
                val typesAreEqual = mkEq(symType, mkInt(findTypeId(it)))
                val numDimensions = mkEq(symNumDimensions.toIntValue(), mkInt(objectNumDimensions).toIntValue())
                constraints += mkOr(mkNot(typesAreEqual), numDimensions)
            }

        return mkAnd(constraints)
    }

    /**
     * returns constraint representing, that object with address [addr] is parametrized by [types] type parameters.
     * @see UtGenericExpression
     */
    fun genericTypeParameterConstraint(addr: UtAddrExpression, types: List<TypeStorage>) =
        UtGenericExpression(addr, types, numberOfTypes)

    /**
     * returns constraint representing that type parameters of an object with address [firstAddr] are equal to
     * type parameters of an object with address [secondAddr], corresponding to [indexInjection]
     * @see UtEqGenericTypeParametersExpression
     */
    @Suppress("unused")
    fun eqGenericTypeParametersConstraint(
        firstAddr: UtAddrExpression,
        secondAddr: UtAddrExpression,
        vararg indexInjection: Pair<Int, Int>
    ): UtEqGenericTypeParametersExpression {
        setParameterTypeStoragesEquality(firstAddr, secondAddr, indexInjection)

        return UtEqGenericTypeParametersExpression(firstAddr, secondAddr, mapOf(*indexInjection))
    }

    /**
     * returns constraint representing that type parameters of an object with address [firstAddr] are equal to
     * the corresponding type parameters of an object with address [secondAddr]
     * @see UtEqGenericTypeParametersExpression
     */
    fun eqGenericTypeParametersConstraint(
        firstAddr: UtAddrExpression,
        secondAddr: UtAddrExpression,
        parameterSize: Int
    ) : UtEqGenericTypeParametersExpression {
        val injections = Array(parameterSize) { it to it }

        return eqGenericTypeParametersConstraint(firstAddr, secondAddr, *injections)
    }

    /**
     * returns constraint representing that the first type parameter of an object with address [firstAddr] are equal to
     * the first type parameter of an object with address [secondAddr]
     * @see UtEqGenericTypeParametersExpression
     */
    fun eqGenericSingleTypeParameterConstraint(firstAddr: UtAddrExpression, secondAddr: UtAddrExpression) =
        eqGenericTypeParametersConstraint(firstAddr, secondAddr, 0 to 0)

    /**
     * Associates provided [typeStorages] with an object with the provided [addr].
     */
    fun saveObjectParameterTypeStorages(addr: UtAddrExpression, typeStorages: List<TypeStorage>) {
        genericTypeStorageByAddr += addr to typeStorages
    }

    /**
     * Retrieves parameter type storages of an object with the given [addr] if present, or null otherwise.
     */
    fun getTypeStoragesForObjectTypeParameters(addr: UtAddrExpression): List<TypeStorage>? = genericTypeStorageByAddr[addr]

    /**
     * Set types storages for [firstAddr]'s type parameters equal to type storages for [secondAddr]'s type parameters
     * according to provided types injection represented by [indexInjection].
     */
    private fun setParameterTypeStoragesEquality(
        firstAddr: UtAddrExpression,
        secondAddr: UtAddrExpression,
        indexInjection: Array<out Pair<Int, Int>>
    ) {
        val existingGenericTypes = genericTypeStorageByAddr[secondAddr] ?: return

        val currentGenericTypes = mutableMapOf<Int, TypeStorage>()

        indexInjection.forEach { (from, to) ->
            require(from >= 0 && from < existingGenericTypes.size) {
                "Type injection is out of bounds: should be in [0; ${existingGenericTypes.size}) but is $from"
            }

            currentGenericTypes[to] = existingGenericTypes[from]
        }

        genericTypeStorageByAddr[firstAddr] = currentGenericTypes
            .entries
            .sortedBy { it.key }
            .mapTo(mutableListOf()) { it.value }
    }

    /**
     * Returns constraint representing that an object with address [addr] has the same type as the type parameter
     * with index [i] of an object with address [baseAddr].
     *
     * For a SomeCollection<A, B> the type parameters are [A, B], where A and B are type variables
     * with indices zero and one respectively. To connect some element of the collection with its generic type
     * add to the constraints `typeConstraintToGenericTypeParameter(elementAddr, collectionAddr, typeParamIndex)`.
     *
     * @see UtIsGenericTypeExpression
     */
    fun typeConstraintToGenericTypeParameter(
        addr: UtAddrExpression,
        baseAddr: UtAddrExpression,
        i: Int
    ): UtIsGenericTypeExpression = UtIsGenericTypeExpression(addr, baseAddr, i)

    /**
     * Looks for a substitution for the given [method].
     *
     * @param method a method to be substituted.
     * @return substituted method if the given [method] has substitution, null otherwise.
     *
     * Note: all the methods in the class with substitutions will be returned instead of methods of the target class
     * with the same name and parameters' types without any additional annotations. The only exception is `<init>`
     * method, substitutions will be returned only for constructors marked by [org.utbot.api.annotation.UtConstructorMock]
     * annotation.
     */
    fun findSubstitutionOrNull(method: SootMethod): SootMethod? {
        val declaringClass = method.declaringClass
        val classWithSubstitutions = targetToSubstitution[declaringClass]

        val substitutedMethod = classWithSubstitutions
            ?.methods
            ?.singleOrNull { it.name == method.name && it.parameterTypes == method.parameterTypes }
        // Note: subSignature is not used in order to support `this` as method's return value.
        // Otherwise we'd have to check for wrong `this` type in the subSignature

        if (method.isConstructor) {
            // if the constructor doesn't have the mock annotation do not substitute it
            substitutedMethod?.findMockAnnotationOrNull ?: return null
        }
        return substitutedMethod
    }

    /**
     * Returns a class containing substitutions for the methods belong to the target class, null if there is not such class.
     */
    @Suppress("unused")
    fun findSubstitutionByTargetOrNull(targetClass: SootClass): SootClass? = targetToSubstitution[targetClass]

    /**
     * Returns a target class by given class with methods substitutions.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun findTargetBySubstitutionOrNull(classWithSubstitutions: SootClass): SootClass? =
        substitutionToTarget[classWithSubstitutions]

    /**
     * Looks for 'real' type.
     *
     * For example, we have two classes: A and B, B contains substitutions for A's methods.
     * `findRealType(a.type)` will return `a.type`, but `findRealType(b.type)` will return `a.type` as well.
     *
     * Returns:
     * * [type] if it is not a RefType;
     * * [type] if it is a RefType and it doesn't have a target class to substitute;
     * * otherwise a type of the target class, which methods should be substituted.
     */
    fun findRealType(type: Type): Type =
        if (type !is RefType) type else findTargetBySubstitutionOrNull(type.sootClass)?.type ?: type

    /**
     * Returns a select expression containing information about whether [ClassCastException] is allowed or not
     * for an object with the given [addr].
     *
     * True means that [ClassCastException] might be thrown, false will restrict it.
     */
    fun isClassCastExceptionAllowed(addr: UtAddrExpression) = isClassCastExceptionAllowed.select(addr)

    /**
     * Modify [isClassCastExceptionAllowed] to make impossible for a [ClassCastException] to be thrown for an object
     * with the given [addr].
     */
    fun disableCastClassExceptionCheck(addr: UtAddrExpression) {
        isClassCastExceptionAllowed = isClassCastExceptionAllowed.store(addr, UtFalse)
    }

    /**
     * Returns chunkId for the given [arrayType].
     *
     * Examples:
     * * Object[] -> RefValues_Arrays
     * * int[] -> intArrays
     * * int[][] -> MultiArrays
     */
    fun arrayChunkId(arrayType: ArrayType) = when (arrayType.numDimensions) {
        1 -> if (arrayType.baseType is RefType) {
            ChunkId("RefValues", "Arrays")
        } else {
            ChunkId("${findRealType(arrayType.baseType)}", "Arrays")
        }
        else -> ChunkId("Multi", "Arrays")
    }

    companion object {
        // we use different shifts to distinguish easily types from objects in z3 listings
        const val objectCounterInitialValue = 0x00000001 // 0x00000000 is reserved for NULL

        // we want to reserve addresses for every ClassRef in the program starting from this one
        // Note: the number had been chosen randomly and can be changes without any consequences
        const val classRefAddrsInitialValue = -16777216 // -(2 ^ 24)

        // since we use typeId as addr for ConstRef, we can not use 0x00000000 because of NULL value
        const val typeCounterInitialValue = 0x00000001
        const val symbolicReturnNameCounterInitialValue = 0x80000000
        const val objectNumDimensions = 0
        const val emptyTypeId = 0
        private const val primitivesNumber = 8

        internal val primTypes
            get() = listOf(
                ByteType.v(),
                ShortType.v(),
                IntType.v(),
                LongType.v(),
                FloatType.v(),
                DoubleType.v(),
                BooleanType.v(),
                CharType.v()
            )

        val numberOfTypes get() = Scene.v().classes.size + primitivesNumber + typeCounterInitialValue

        /**
         * Stores [TypeStorage] for [OBJECT_TYPE]. As it should be changed when Soot scene changes,
         * it is loaded each time when [TypeRegistry] is created in init section.
         */
        lateinit var objectTypeStorage: TypeStorage
    }
}

private fun initialArray(descriptor: MemoryChunkDescriptor) =
    mkArrayConst(descriptor.id.toId(), UtAddrSort, descriptor.itemSort())

/**
 * Creates item sort for memory chunk descriptor.
 *
 * If the given type is ArrayType, i.e. int[][], we have to create sort for it.
 * Otherwise, there is two options: either type is a RefType and it's elementType is Array, so the corresponding
 * array stores addresses, or it stores primitive values themselves.
 */
private fun MemoryChunkDescriptor.itemSort(): UtSort = when (type) {
    is ArrayType -> type.toSort()
    else -> if (elementType is ArrayType) UtAddrSort else elementType.toSort()
}

enum class MemoryState { INITIAL, STATIC_INITIAL, CURRENT }

data class LocalMemoryUpdate(
    val locals: PersistentMap<LocalVariable, SymbolicValue?> = persistentHashMapOf(),
) {
    operator fun plus(other: LocalMemoryUpdate) =
        this.copy(
            locals = locals.putAll(other.locals),
        )
}

data class MemoryUpdate(
    val stores: PersistentList<UtNamedStore> = persistentListOf(),
    val touchedChunkDescriptors: PersistentSet<MemoryChunkDescriptor> = persistentSetOf(),
    val concrete: PersistentMap<UtAddrExpression, Concrete> = persistentHashMapOf(),
    val mockInfos: PersistentList<MockInfoEnriched> = persistentListOf(),
    val staticInstanceStorage: PersistentMap<ClassId, ObjectValue> = persistentHashMapOf(),
    val initializedStaticFields: PersistentSet<FieldId> = persistentHashSetOf(),
    val staticFieldsUpdates: PersistentList<StaticFieldMemoryUpdateInfo> = persistentListOf(),
    val meaningfulStaticFields: PersistentSet<FieldId> = persistentHashSetOf(),
    val addrToArrayType: PersistentMap<UtAddrExpression, ArrayType> = persistentHashMapOf(),
    val addrToMockInfo: PersistentMap<UtAddrExpression, UtMockInfo> = persistentHashMapOf(),
    val visitedValues: PersistentList<UtAddrExpression> = persistentListOf(),
    val touchedAddresses: PersistentList<UtAddrExpression> = persistentListOf(),
    val classIdToClearStatics: ClassId? = null,
    val instanceFieldReads: PersistentSet<InstanceFieldReadOperation> = persistentHashSetOf(),
    val speculativelyNotNullAddresses: PersistentList<UtAddrExpression> = persistentListOf(),
    val symbolicEnumValues: PersistentList<ObjectValue> = persistentListOf()
) {
    operator fun plus(other: MemoryUpdate) =
        this.copy(
            stores = stores.addAll(other.stores),
            touchedChunkDescriptors = touchedChunkDescriptors.addAll(other.touchedChunkDescriptors),
            concrete = concrete.putAll(other.concrete),
            mockInfos = mockInfos.mergeWithUpdate(other.mockInfos),
            staticInstanceStorage = staticInstanceStorage.putAll(other.staticInstanceStorage),
            initializedStaticFields = initializedStaticFields.addAll(other.initializedStaticFields),
            staticFieldsUpdates = staticFieldsUpdates.addAll(other.staticFieldsUpdates),
            meaningfulStaticFields = meaningfulStaticFields.addAll(other.meaningfulStaticFields),
            addrToArrayType = addrToArrayType.putAll(other.addrToArrayType),
            addrToMockInfo = addrToMockInfo.putAll(other.addrToMockInfo),
            visitedValues = visitedValues.addAll(other.visitedValues),
            touchedAddresses = touchedAddresses.addAll(other.touchedAddresses),
            classIdToClearStatics = other.classIdToClearStatics,
            instanceFieldReads = instanceFieldReads.addAll(other.instanceFieldReads),
            speculativelyNotNullAddresses = speculativelyNotNullAddresses.addAll(other.speculativelyNotNullAddresses),
            symbolicEnumValues = symbolicEnumValues.addAll(other.symbolicEnumValues),
        )

    fun getSymbolicEnumValues(classId: ClassId): List<ObjectValue> =
        symbolicEnumValues.filter { it.type.classId == classId }
}

// array - Java Array
// chunk - Memory Model (convenient for Solver)
//       - solver's (get-model) results to parse

/**
 * In current implementation it references
 * to SMT solver's array used for storage of some
 *
 * [id] is typically corresponds to Solver's array name
 * //TODO docs for 3 cases: array of primitives, array of objects, object's fields (including static)
 */
data class MemoryChunkDescriptor(
    val id: ChunkId,
    val type: RefLikeType,
    val elementType: Type
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemoryChunkDescriptor

        return id == other.id
    }

    override fun hashCode() = id.hashCode()
}

data class ChunkId(val type: String, val field: String) {
    fun toId() = "${type}_$field"
}

data class LocalVariable(val name: String)

data class UtNamedStore(
    val chunkDescriptor: MemoryChunkDescriptor,
    val index: UtExpression,
    val value: UtExpression
)

/**
 * Create [UtNamedStore] with simplified [index] and [value] expressions.
 *
 * @see RewritingVisitor
 */
fun simplifiedNamedStore(
    chunkDescriptor: MemoryChunkDescriptor,
    index: UtExpression,
    value: UtExpression
) = RewritingVisitor().let { visitor -> UtNamedStore(chunkDescriptor, index.accept(visitor), value.accept(visitor)) }

/**
 * Updates persistent map where value = null in update means deletion of original key-value
 */
private fun <K, V> PersistentMap<K, V>.update(update: Map<K, V?>): PersistentMap<K, V> {
    if (update.isEmpty()) return this
    val deletions = mutableListOf<K>()
    val updates = mutableMapOf<K, V>()
    update.forEach { (name, value) ->
        if (value == null) {
            deletions.add(name)
        } else {
            updates[name] = value
        }
    }
    return this.mutate { map ->
        deletions.forEach { map.remove(it) }
        map.putAll(updates)
    }
}

fun localMemoryUpdate(vararg updates: Pair<LocalVariable, SymbolicValue?>) =
    LocalMemoryUpdate(
        locals = persistentHashMapOf(*updates)
    )

private val STRING_INTERNAL = ChunkId(java.lang.String::class.qualifiedName!!, "internal")

private val NATIVE_STRING_VALUE = ChunkId(UtNativeString::class.qualifiedName!!, "value")

internal val STRING_LENGTH
    get() = utStringClass.getField("length", IntType.v())
internal val STRING_VALUE
    get() = utStringClass.getField("value", CharType.v().arrayType)

/**
 * Map to support internal string representation, addr -> String
 */
internal val STRING_INTERNAL_DESCRIPTOR: MemoryChunkDescriptor
    get() = MemoryChunkDescriptor(STRING_INTERNAL, STRING_TYPE, SeqType)


internal val NATIVE_STRING_VALUE_DESCRIPTOR: MemoryChunkDescriptor
    get() = MemoryChunkDescriptor(NATIVE_STRING_VALUE, utNativeStringClass.type, SeqType)

/**
 * Returns internal string representation by String object address, addr -> String
 */
fun Memory.nativeStringValue(addr: UtAddrExpression) =
    PrimitiveValue(SeqType, findArray(NATIVE_STRING_VALUE_DESCRIPTOR).select(addr)).expr

private const val STRING_INTERN_MAP_LABEL = "java.lang.String_intern_map"

/**
 * Map to support string internation process, String -> addr
 */
internal val STRING_INTERN_MAP = mkArrayConst(STRING_INTERN_MAP_LABEL, UtSeqSort, UtAddrSort)

/**
 * Returns interned string, using map String -> addr
 *
 * Note: working with this map requires additional assert on internal string maps
 *
 * @see NATIVE_STRING_VALUE_DESCRIPTOR
 */
fun internString(string: UtExpression): UtAddrExpression = UtAddrExpression(STRING_INTERN_MAP.select(string))

/**
 *
 */
private fun List<MockInfoEnriched>.mergeWithUpdate(other: List<MockInfoEnriched>): PersistentList<MockInfoEnriched> {
    // map from MockInfo to MockInfoEnriched
    val updates = other.associateByTo(mutableMapOf()) { it.mockInfo }

    // create list from original values, then merge executables from update
    val mergeResult = this.mapTo(mutableListOf()) { original ->
        original + updates.remove(original.mockInfo)
    }

    // add tail: values from updates for which their MockInfo didn't present in [this] yet.
    mergeResult += updates.values

    return mergeResult.toPersistentList()
}

/**
 * Copies executables from [update] to the executables of [this] object.
 */
private operator fun MockInfoEnriched.plus(update: MockInfoEnriched?): MockInfoEnriched {
    if (update == null || update.executables.isEmpty()) return this

    require(mockInfo == update.mockInfo)

    return this.copy(executables = executables.toMutableMap().mergeValues(update.executables))
}

private fun <K, V> MutableMap<K, List<V>>.mergeValues(other: Map<K, List<V>>): Map<K, List<V>> = apply {
    other.forEach { (key, values) -> merge(key, values) { v1, v2 -> v1 + v2 } }
}
