package org.utbot.engine

import org.utbot.engine.MemoryState.CURRENT
import org.utbot.engine.MemoryState.INITIAL
import org.utbot.engine.MemoryState.STATIC_INITIAL
import org.utbot.engine.pc.UtAddrExpression
import org.utbot.engine.pc.UtAddrSort
import org.utbot.engine.pc.UtArrayExpressionBase
import org.utbot.engine.pc.UtArraySelectExpression
import org.utbot.engine.pc.UtArraySort
import org.utbot.engine.pc.UtBoolSort
import org.utbot.engine.pc.UtConstArrayExpression
import org.utbot.engine.pc.UtExpression
import org.utbot.engine.pc.UtFalse
import org.utbot.engine.pc.UtInt32Sort
import org.utbot.engine.pc.UtIntSort
import org.utbot.engine.pc.UtMkArrayExpression
import org.utbot.engine.pc.UtMkTermArrayExpression
import org.utbot.engine.pc.UtSeqSort
import org.utbot.engine.pc.UtSort
import org.utbot.engine.pc.UtTrue
import org.utbot.engine.pc.mkArrayConst
import org.utbot.engine.pc.mkInt
import org.utbot.engine.pc.select
import org.utbot.engine.pc.store
import org.utbot.engine.pc.toSort
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
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
import org.utbot.engine.types.STRING_TYPE
import org.utbot.engine.types.SeqType
import org.utbot.engine.types.TypeResolver
import org.utbot.framework.plugin.api.classId
import soot.ArrayType
import soot.CharType
import soot.IntType
import soot.RefLikeType
import soot.Scene
import soot.SootField
import soot.Type


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
 * [fieldValues] stores symbolic values for specified fields of the concrete object instances.
 * We need to associate field of a concrete instance with the created symbolic value to not lose an information about its type.
 * For example, if field's declared type is Runnable but at the current state it is a specific lambda,
 * we have to save this lambda as a type of this field to be able to retrieve it in the future.
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
    private val fieldValues: PersistentMap<SootField, PersistentMap<UtAddrExpression, SymbolicValue>> = persistentHashMapOf(),
    private val addrToArrayType: PersistentMap<UtAddrExpression, ArrayType> = persistentHashMapOf(),
    private val genericTypeStorageByAddr: PersistentMap<UtAddrExpression, List<TypeStorage>> = persistentHashMapOf(),
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
     * Retrieves parameter type storages of an object with the given [addr] if present, or null otherwise.
     */
    fun getTypeStoragesForObjectTypeParameters(
        addr: UtAddrExpression
    ): List<TypeStorage>? = genericTypeStorageByAddr[addr]

    /**
     * Returns all collected information about addresses and corresponding generic types.
     */
    fun getAllGenericTypeInfo(): Map<UtAddrExpression, List<TypeStorage>> = genericTypeStorageByAddr

    /**
     * Returns a symbolic value, associated with the specified [field] of the object with the specified [instanceAddr],
     * if present, and null otherwise.
     */
    fun fieldValue(field: SootField, instanceAddr: UtAddrExpression): SymbolicValue? =
        fieldValues[field]?.get(instanceAddr)

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
        val previousFieldValues = fieldValues.toMutableMap()


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

                if (sootField.isStatic) {
                    // Only statics should be cleared here
                    previousFieldValues.remove(sootField)
                }
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

        // We have a list with updates for generic type info, and we want to apply
        // them in such way that only updates with more precise type information
        // should be applied.
        val currentGenericsMap = update
            .genericTypeStorageByAddr
            // go over type generic updates and apply them to already existed info
            .fold(genericTypeStorageByAddr.toMutableMap()) { acc, value ->
                // If we have more type information, a new type storage will be returned.
                // Otherwise, we will have the same info taken from the memory.
                val (addr, typeStorages) =
                    TypeResolver.createGenericTypeInfoUpdate(value.first, value.second, acc)
                        .genericTypeStorageByAddr
                        .single()
                acc[addr] = typeStorages
                acc
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
            fieldValues = previousFieldValues.toPersistentMap().putAll(update.fieldValues),
            addrToArrayType = addrToArrayType.putAll(update.addrToArrayType),
            genericTypeStorageByAddr = currentGenericsMap.toPersistentMap(),
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
        staticFieldsUpdates = updates.staticFieldsUpdates,
        fieldValues = updates.fieldValues.filter { it.key.isStatic }.toPersistentMap()
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
    val fieldValues: PersistentMap<SootField, PersistentMap<UtAddrExpression, SymbolicValue>> = persistentHashMapOf(),
    val addrToArrayType: PersistentMap<UtAddrExpression, ArrayType> = persistentHashMapOf(),
    val genericTypeStorageByAddr: PersistentList<Pair<UtAddrExpression, List<TypeStorage>>> = persistentListOf(),
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
            fieldValues = fieldValues.putAll(other.fieldValues),
            addrToArrayType = addrToArrayType.putAll(other.addrToArrayType),
            genericTypeStorageByAddr = genericTypeStorageByAddr.addAll(other.genericTypeStorageByAddr),
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
 * Create [UtNamedStore] with unsimplified [index] and [value] expressions.
 *
 * @note simplifications occur explicitly in [Traverser]
 */
fun namedStore(
    chunkDescriptor: MemoryChunkDescriptor,
    index: UtExpression,
    value: UtExpression
) = UtNamedStore(chunkDescriptor, index, value)

/**
 * Updates persistent map where value = null in update means deletion of original key-value
 */
fun <K, V> PersistentMap<K, V>.update(update: Map<K, V?>): PersistentMap<K, V> {
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

internal val STRING_LENGTH
    get() = utStringClass.getField("length", IntType.v())
internal val STRING_VALUE
    get() = utStringClass.getField("value", CharType.v().arrayType)

/**
 * Map to support internal string representation, addr -> String
 */
internal val STRING_INTERNAL_DESCRIPTOR: MemoryChunkDescriptor
    get() = MemoryChunkDescriptor(STRING_INTERNAL, STRING_TYPE, SeqType)


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
