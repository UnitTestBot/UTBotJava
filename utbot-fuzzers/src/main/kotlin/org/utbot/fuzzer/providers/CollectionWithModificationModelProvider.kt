package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.fuzzNumbers
import org.utbot.fuzzer.objects.create

class CollectionWithModificationModelProvider(
    idGenerator: IdentityPreservingIdGenerator<Int>,
    recursionDepthLeft: Int = 2,
    private var defaultModificationCount: IntArray = intArrayOf(0, 1, 3)
) : RecursiveModelProvider(idGenerator, recursionDepthLeft) {

    init {
        totalLimit = 100_000
    }

    // List of available implementations with modification method to insert values
    // Should be listed from more specific interface to more general,
    // because suitable info is searched by the list.
    private val modifications = listOf(
        // SETS
        Info(java.util.NavigableSet::class.id, java.util.TreeSet::class.id, "add", listOf(objectClassId), booleanClassId) {
            it.size == 1 && it[0].classId.isSubtypeOfWithReflection(java.lang.Comparable::class.id)
        },
        Info(java.util.SortedSet::class.id, java.util.TreeSet::class.id, "add", listOf(objectClassId), booleanClassId) {
            it.size == 1 && it[0].classId.isSubtypeOfWithReflection(java.lang.Comparable::class.id)
        },
        Info(java.util.Set::class.id, java.util.HashSet::class.id, "add", listOf(objectClassId), booleanClassId),
        // QUEUES
        Info(java.util.Queue::class.id, java.util.ArrayDeque::class.id, "add", listOf(objectClassId), booleanClassId),
        Info(java.util.Deque::class.id, java.util.ArrayDeque::class.id, "add", listOf(objectClassId), booleanClassId),
        Info(java.util.Stack::class.id, java.util.Stack::class.id, "push", listOf(objectClassId), booleanClassId),
        // LISTS
        Info(java.util.List::class.id, java.util.ArrayList::class.id, "add", listOf(objectClassId), booleanClassId),
        // MAPS
        Info(java.util.NavigableMap::class.id, java.util.TreeMap::class.id, "put", listOf(objectClassId, objectClassId), objectClassId) {
            it.size == 2 && it[0].classId.isSubtypeOfWithReflection(java.lang.Comparable::class.id)
        },
        Info(java.util.SortedMap::class.id, java.util.TreeMap::class.id, "put", listOf(objectClassId, objectClassId), objectClassId) {
            it.size == 2 && it[0].classId.isSubtypeOfWithReflection(java.lang.Comparable::class.id)
        },
        Info(java.util.Map::class.id, java.util.HashMap::class.id, "put", listOf(objectClassId, objectClassId), objectClassId),
        // ITERABLE
        Info(java.util.Collection::class.id, java.util.ArrayList::class.id, "add", listOf(objectClassId), booleanClassId),
        Info(java.lang.Iterable::class.id, java.util.ArrayList::class.id, "add", listOf(objectClassId), booleanClassId),
    )
    private var modificationCount = 7

    override fun newInstance(parentProvider: RecursiveModelProvider, constructor: ModelConstructor): RecursiveModelProvider {
        val newInstance = CollectionWithModificationModelProvider(
            parentProvider.idGenerator, parentProvider.recursionDepthLeft - 1
        )
        newInstance.copySettings(parentProvider)
        if (parentProvider is CollectionWithModificationModelProvider) {
            newInstance.defaultModificationCount = parentProvider.defaultModificationCount
        }
        return newInstance
    }

    override fun generateModelConstructors(
        description: FuzzedMethodDescription,
        parameterIndex: Int,
        classId: ClassId,
    ): Sequence<ModelConstructor> {

        val info: Info? = if (!classId.isAbstract) {
            when {
                classId.isSubtypeOfWithReflection(Collection::class.id) -> Info(classId, classId, "add", listOf(objectClassId), booleanClassId)
                classId.isSubtypeOfWithReflection(Map::class.id) -> Info(classId, classId, "put", listOf(objectClassId, objectClassId), objectClassId)
                else -> null
            }
        } else {
            modifications.find {
                classId == it.superClass
            }
        }

        val sequence = info?.let {
            val genericTypes = description.fuzzerType(parameterIndex)?.generics ?: emptyList()
            if (genericTypes.isNotEmpty()) {
                // this check removes cases when TreeSet or TreeMap is created without comparable key
                val lengths = if (info.canModify(genericTypes)) {
                    fuzzNumbers(description.concreteValues, *defaultModificationCount) { it in 1..modificationCount }
                } else {
                    sequenceOf(0)
                }
                lengths.map { length ->
                    ModelConstructor(genericTypes, repeat = length) { values ->
                        info.assembleModel(info.concreteClass, values)
                    }
                }
            } else {
                emptySequence()
            }
        }
        return sequence ?: emptySequence()
    }

    private fun Info.assembleModel(concreteClassId: ClassId, values: List<FuzzedValue>): FuzzedValue {
        return concreteClassId.create {
            id = { idGenerator.createId() }
            using empty constructor
            val paramCount = params.size
            values.asSequence()
                .windowed(paramCount, paramCount)
                .forEach { each ->
                    call instance method(
                        methodName,
                        params,
                        returnType
                    ) with values(*Array(paramCount) { each[it].model })
                }
        }.fuzzed {
            summary = "%var% = test collection"
        }
    }

    private class Info(
        val superClass: ClassId,
        val concreteClass: ClassId,
        val methodName: String,
        val params: List<ClassId>,
        val returnType: ClassId = voidClassId,
        val canModify: (List<FuzzedType>) -> Boolean = { true }
    )

    private fun ClassId.isSubtypeOfWithReflection(another: ClassId): Boolean {
        // commented code above doesn't work this case: SomeList<T> extends LinkedList<T> {} and Collection
//        return isSubtypeOf(another)
        return another.jClass.isAssignableFrom(this.jClass)
    }
}