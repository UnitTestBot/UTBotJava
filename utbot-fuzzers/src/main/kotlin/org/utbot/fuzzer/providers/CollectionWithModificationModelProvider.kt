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
import org.utbot.fuzzer.types.*

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
        Info(java.util.NavigableSet::class.id.toJavaType(), java.util.TreeSet::class.id.toJavaType(), "add", listOf(JavaObject), JavaBool) {
            it.size == 1 && it[0].type.isSubtypeOfWithReflection(java.lang.Comparable::class.id)
        },
        Info(java.util.SortedSet::class.id.toJavaType(), java.util.TreeSet::class.id.toJavaType(), "add", listOf(JavaObject), JavaBool) {
            it.size == 1 && it[0].type.isSubtypeOfWithReflection(java.lang.Comparable::class.id)
        },
        Info(java.util.Set::class.id.toJavaType(), java.util.HashSet::class.id.toJavaType(), "add", listOf(JavaObject), JavaBool),
        // QUEUES
        Info(java.util.Queue::class.id.toJavaType(), java.util.ArrayDeque::class.id.toJavaType(), "add", listOf(JavaObject), JavaBool),
        Info(java.util.Deque::class.id.toJavaType(), java.util.ArrayDeque::class.id.toJavaType(), "add", listOf(JavaObject), JavaBool),
        Info(java.util.Stack::class.id.toJavaType(), java.util.Stack::class.id.toJavaType(), "push", listOf(JavaObject), JavaBool),
        // LISTS
        Info(java.util.List::class.id.toJavaType(), java.util.ArrayList::class.id.toJavaType(), "add", listOf(JavaObject), JavaBool),
        // MAPS
        Info(java.util.NavigableMap::class.id.toJavaType(), java.util.TreeMap::class.id.toJavaType(), "put", listOf(JavaObject, JavaObject), JavaObject) {
            it.size == 2 && it[0].type.isSubtypeOfWithReflection(java.lang.Comparable::class.id)
        },
        Info(java.util.SortedMap::class.id.toJavaType(), java.util.TreeMap::class.id.toJavaType(), "put", listOf(JavaObject, JavaObject), JavaObject) {
            it.size == 2 && it[0].type.isSubtypeOfWithReflection(java.lang.Comparable::class.id)
        },
        Info(java.util.Map::class.id.toJavaType(), java.util.HashMap::class.id.toJavaType(), "put", listOf(JavaObject, JavaObject), JavaObject),
        // ITERABLE
        Info(java.util.Collection::class.id.toJavaType(), java.util.ArrayList::class.id.toJavaType(), "add", listOf(JavaObject), JavaBool),
        Info(java.lang.Iterable::class.id.toJavaType(), java.util.ArrayList::class.id.toJavaType(), "add", listOf(JavaObject), JavaBool),
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
        type: Type,
    ): Sequence<ModelConstructor> {
        if (type !is WithClassId) {
            return emptySequence()
        }
        val info: Info? = if (!type.classId.isAbstract) {
            when {
                type.isSubtypeOfWithReflection(Collection::class.id) -> Info(type, type, "add", listOf(JavaObject), JavaBool)
                type.isSubtypeOfWithReflection(Map::class.id) -> Info(type, type, "put", listOf(JavaObject, JavaObject), JavaObject)
                else -> null
            }
        } else {
            modifications.find {
                type == it.superClass
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
                        info.assembleModel((info.concreteClass as WithClassId).classId, values)
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
                        params.map { (it as WithClassId).classId },
                        (returnType as WithClassId).classId
                    ) with values(*Array(paramCount) { each[it].model })
                }
        }.fuzzed {
            summary = "%var% = test collection"
        }
    }

    private class Info(
        val superClass: Type,
        val concreteClass: Type,
        val methodName: String,
        val params: List<Type>,
        val returnType: Type,
        val canModify: (List<FuzzedType>) -> Boolean = { true }
    )

    private fun Type.isSubtypeOfWithReflection(another: ClassId): Boolean {
        if (this !is WithClassId) return false
        // commented code above doesn't work this case: SomeList<T> extends LinkedList<T> {} and Collection
//        return isSubtypeOf(another)
        return another.jClass.isAssignableFrom(classId.jClass)
    }
}