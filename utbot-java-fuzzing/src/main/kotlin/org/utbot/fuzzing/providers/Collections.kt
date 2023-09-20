package org.utbot.fuzzing.providers

import com.google.common.reflect.TypeToken
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.*
import org.utbot.fuzzing.spring.utils.jType
import org.utbot.fuzzing.utils.hex
import java.lang.reflect.Method
import kotlin.reflect.KClass

class EmptyCollectionValueProvider(
    val idGenerator: IdGenerator<Int>
) : JavaValueProvider {
    private class Info(val classId: ClassId, val methodName: String, val returnType: ClassId = classId)

    private val unmodifiableCollections = listOf(
        Info(java.util.List::class.id, "emptyList"),
        Info(java.util.Set::class.id, "emptySet"),
        Info(java.util.Map::class.id, "emptyMap"),
        Info(java.util.Collection::class.id, "emptyList", returnType = java.util.List::class.id),
        Info(java.lang.Iterable::class.id, "emptyList", returnType = java.util.List::class.id),
        Info(java.util.Iterator::class.id, "emptyIterator"),
    )

    private val emptyCollections = listOf(
        Info(java.util.NavigableSet::class.id, "constructor", java.util.TreeSet::class.id),
        Info(java.util.SortedSet::class.id, "constructor", java.util.TreeSet::class.id),
        Info(java.util.NavigableMap::class.id, "constructor", java.util.TreeMap::class.id),
        Info(java.util.SortedMap::class.id, "constructor", java.util.TreeMap::class.id),
    )

    override fun generate(description: FuzzedDescription, type: FuzzedType) = sequence {
        unmodifiableCollections
            .asSequence()
            .filter { it.classId == type.classId }
            .forEach { info ->
                yieldWith(info.classId, MethodId(java.util.Collections::class.id, info.methodName, info.returnType, emptyList()))
            }
        emptyCollections
            .asSequence()
            .filter { it.classId == type.classId }
            .forEach { info ->
                yieldWith(info.classId, ConstructorId(info.returnType, emptyList()))
            }
    }

    private suspend fun SequenceScope<Seed<FuzzedType, FuzzedValue>>.yieldWith(classId: ClassId, executableId: ExecutableId) {
        yield(Seed.Recursive(
            construct = Routine.Create(executableId.parameters.map { FuzzedType(it) }) { value ->
                UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = classId,
                    modelName = "",
                    instantiationCall = UtExecutableCallModel(null, executableId, value.map { it.model })
                ).fuzzed {
                    summary = "%var% = ${executableId.classId.simpleName}#${executableId.name}"
                }
            },
            empty = Routine.Empty {
                if (executableId.parameters.isEmpty())  {
                    UtAssembleModel(
                        id = idGenerator.createId(),
                        classId = classId,
                        modelName = "",
                        instantiationCall = UtExecutableCallModel(null, executableId, emptyList())

                    ).fuzzed{
                        summary = "%var% = ${executableId.classId.simpleName}#${executableId.name}"
                    }
                } else {
                    nullFuzzedValue(classId)
                }
            },
        ))
    }
}

class MapValueProvider(
    idGenerator: IdGenerator<Int>
) : CollectionValueProvider(idGenerator, java.util.Map::class.id) {

    private enum class MethodCall { KEYS, VALUES }

    private fun findTypeByMethod(description: FuzzedDescription, type: FuzzedType, method: MethodCall): FuzzedType {
        val methodName = when (method) {
            MethodCall.KEYS -> "keySet"
            MethodCall.VALUES -> "values"
        }
        val m = Map::class.java.getMethod(methodName)
        return resolveTypeByMethod(description, type, m)?.let {
            assert(it.classId.isSubtypeOf(collectionClassId))
            assert(it.generics.size == 1)
            it.generics[0]
        } ?: FuzzedType(objectClassId)
    }

    override fun resolveType(description: FuzzedDescription, type: FuzzedType) = sequence {
        val keyGeneric = findTypeByMethod(description, type, MethodCall.KEYS)
        val valueGeneric = findTypeByMethod(description, type, MethodCall.VALUES)
        when (type.classId) {
            java.util.Map::class.id -> {
                if (keyGeneric.classId isSubtypeOf Comparable::class) {
                    yield(FuzzedType(java.util.TreeMap::class.id, listOf(keyGeneric, valueGeneric)))
                }
                yield(FuzzedType(java.util.HashMap::class.id, listOf(keyGeneric, valueGeneric)))
            }
            java.util.NavigableMap::class.id,
            java.util.SortedMap::class.id -> {
                if (keyGeneric.classId isSubtypeOf Comparable::class) {
                    yield(FuzzedType(java.util.TreeMap::class.id, listOf(keyGeneric, valueGeneric)))
                }
            }
            else -> yieldConcreteClass(FuzzedType(type.classId, listOf(keyGeneric, valueGeneric)))
        }
    }

    override fun findMethod(resolvedType: FuzzedType, values: List<FuzzedValue>): MethodId {
        return MethodId(resolvedType.classId, "put", objectClassId, listOf(objectClassId, objectClassId))
    }
}

class ListSetValueProvider(
    idGenerator: IdGenerator<Int>
) : CollectionValueProvider(idGenerator, java.util.Collection::class.id) {

    private val iteratorClassId = java.util.Iterator::class.java.id

    private fun findTypeByMethod(description: FuzzedDescription, type: FuzzedType): FuzzedType {
        val method = java.util.Collection::class.java.getMethod("iterator")
        return resolveTypeByMethod(description, type, method)?.let {
            assert(it.classId.isSubtypeOf(iteratorClassId))
            assert(it.generics.size == 1)
            it.generics[0]
        } ?: FuzzedType(objectClassId)
    }

    override fun resolveType(description: FuzzedDescription, type: FuzzedType) = sequence {
        val generic = findTypeByMethod(description, type)
        when (type.classId) {
            java.util.Queue::class.id,
            java.util.Deque::class.id-> {
                yield(FuzzedType(java.util.ArrayDeque::class.id, listOf(generic)))
            }
            java.util.List::class.id -> {
                yield(FuzzedType(java.util.ArrayList::class.id, listOf(generic)))
                yield(FuzzedType(java.util.LinkedList::class.id, listOf(generic)))
            }
            java.util.Collection::class.id -> {
                yield(FuzzedType(java.util.ArrayList::class.id, listOf(generic)))
                yield(FuzzedType(java.util.HashSet::class.id, listOf(generic)))
            }
            java.util.Set::class.id -> {
                if (generic.classId isSubtypeOf Comparable::class) {
                    yield(FuzzedType(java.util.TreeSet::class.id, listOf(generic)))
                }
                yield(FuzzedType(java.util.HashSet::class.id, listOf(generic)))
            }
            java.util.NavigableSet::class.id,
            java.util.SortedSet::class.id -> {
                if (generic.classId isSubtypeOf Comparable::class) {
                    yield(FuzzedType(java.util.TreeSet::class.id, listOf(generic)))
                }
            }
            else -> yieldConcreteClass(FuzzedType(type.classId, listOf(generic)))
        }
    }

    override fun findMethod(resolvedType: FuzzedType, values: List<FuzzedValue>): MethodId {
        return MethodId(resolvedType.classId, "add", booleanClassId, listOf(objectClassId))
    }
}

/**
 * Accepts only instances of Collection or Map
 */
abstract class CollectionValueProvider(
    private val idGenerator: IdGenerator<Int>,
    vararg acceptableCollectionTypes: ClassId
) : JavaValueProvider {

    private val acceptableCollectionTypes = acceptableCollectionTypes.toList()

    override fun accept(type: FuzzedType): Boolean {
        return with (type.classId) {
            acceptableCollectionTypes.any { acceptableCollectionType ->
                isSubtypeOf(acceptableCollectionType.kClass)
            }
        }
    }

    protected suspend fun SequenceScope<FuzzedType>.yieldConcreteClass(type: FuzzedType) {
        if (with (type.classId) { !isAbstract && isPublic && (!isInner || isStatic) }) {
            val emptyConstructor = type.classId.allConstructors.find { it.parameters.isEmpty() }
            if (emptyConstructor != null && emptyConstructor.isPublic) {
                yield(type)
            }
        }
    }

    /**
     * Can be used to resolve some types using [type] and some method of this type
     */
    protected fun resolveTypeByMethod(description: FuzzedDescription, type: FuzzedType, method: Method): FuzzedType? {
        return try {
            toFuzzerType(
                TypeToken.of(type.jType).resolveType(method.genericReturnType).type,
                description.typeCache
            )
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * Types should be resolved with type parameters
     */
    abstract fun resolveType(description: FuzzedDescription, type: FuzzedType) : Sequence<FuzzedType>

    abstract fun findMethod(resolvedType: FuzzedType, values: List<FuzzedValue>) : MethodId

    override fun generate(description: FuzzedDescription, type: FuzzedType) = sequence<Seed<FuzzedType, FuzzedValue>> {
        resolveType(description, type).forEach { resolvedType ->
            val typeParameter = resolvedType.generics
            yield(Seed.Collection(
                construct = Routine.Collection {
                    UtAssembleModel(
                        id = idGenerator.createId(),
                        classId = resolvedType.classId,
                        modelName = "",
                        instantiationCall = UtExecutableCallModel(
                            null,
                            ConstructorId(resolvedType.classId, emptyList()),
                            emptyList()
                        ),
                        modificationsChainProvider = { mutableListOf() }
                    ).fuzzed {
                        summary = "%var% = collection"
                    }
                },
                modify = Routine.ForEach(typeParameter) { self, _, values ->
                    val model = self.model as UtAssembleModel
                    (model.modificationsChain as MutableList) += UtExecutableCallModel(
                        model,
                        findMethod(resolvedType, values),
                        values.map { it.model }
                    )
                }
            ))
        }
    }

    protected infix fun ClassId.isSubtypeOf(klass: KClass<*>): Boolean {
        // commented code above doesn't work this case: SomeList<T> extends LinkedList<T> {} and Collection
//        return isSubtypeOf(another)
        return klass.java.isAssignableFrom(this.jClass)
    }
}

class IteratorValueProvider(val idGenerator: IdGenerator<Int>) : JavaValueProvider {
    override fun accept(type: FuzzedType): Boolean {
        return type.classId == Iterator::class.id
    }

    override fun generate(description: FuzzedDescription, type: FuzzedType): Sequence<Seed<FuzzedType, FuzzedValue>> {
        val generic = type.generics.firstOrNull() ?: FuzzedType(objectClassId)
        return sequenceOf(Seed.Recursive(
            construct = Routine.Create(listOf(FuzzedType(iterableClassId, listOf(generic)))) { v ->
                val id = idGenerator.createId()
                val iterable = when (val model = v.first().model) {
                    is UtAssembleModel -> model
                    is UtNullModel -> return@Create v.first()
                    else -> error("Model can be only UtNullModel or UtAssembleModel, but $model is met")
                }
                UtAssembleModel(
                    id = id,
                    classId = type.classId,
                    modelName = "iterator#${id.hex()}",
                    instantiationCall = UtExecutableCallModel(
                        instance = iterable,
                        executable = MethodId(iterableClassId, "iterator", type.classId, emptyList()),
                        params = emptyList()
                    )
                ).fuzzed {
                    summary = "%var% = ${iterable.classId.simpleName}#iterator()"
                }
            },
            empty = Routine.Empty {
                val id = idGenerator.createId()
                UtAssembleModel(
                    id = id,
                    classId = type.classId,
                    modelName = "emptyIterator#${id.hex()}",
                    instantiationCall = UtExecutableCallModel(
                        instance = null,
                        executable = MethodId(java.util.Collections::class.id, "emptyIterator", type.classId, emptyList()),
                        params = emptyList()
                    )
                ).fuzzed {
                    summary = "%var% = empty iterator"
                }
            }
        ))
    }
}