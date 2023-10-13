package org.utbot.fuzzing

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.iterableClassId
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzing.samples.Implementations
import org.utbot.fuzzing.samples.Stubs
import java.lang.Number
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import java.util.List
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class JavaTypesTest {

    @Test
    fun `recursive generic types are recognized correctly`() {
        runBlockingWithContext {
            val methods = Stubs::class.java.methods
            val method = methods.first { it.name == "resolve" && it.returnType == Int::class.javaPrimitiveType }
            val typeCache = IdentityHashMap<Type, FuzzedType>()
            val type = toFuzzerType(method.genericParameterTypes.first(), typeCache)
            Assertions.assertEquals(1, typeCache.size)
            Assertions.assertTrue(typeCache.values.all { type === it })
            Assertions.assertEquals(1, type.generics.size)
            Assertions.assertTrue(typeCache.values.all { type.generics[0] === it })

            try {
                // If FuzzerType has implemented `equals` and `hashCode` or is data class,
                // that implements those methods implicitly,
                // then adding it to hash table throws [StackOverflowError]
                val set = HashSet<FuzzedType>()
                set += type
            } catch (soe: StackOverflowError) {
                Assertions.fail(
                    "Looks like FuzzerType implements equals and hashCode, " +
                            "which leads unstable behaviour in recursive generics ", soe
                )
            }
        }
    }

    @Test
    fun `can pass types through`() {
        runBlockingWithContext {
            val cache = HashMap<Type, FuzzedType>()
            val methods = Stubs::class.java.methods
            val method = methods.first { it.name == "types" }
            val types = method.genericParameterTypes.map {
                toFuzzerType(it, cache)
            }
            Assertions.assertEquals(
                3,
                cache.size
            ) { "Cache should contain following types: List<Number>, Number and T[] for $method" }
            Assertions.assertTrue(cache.keys.any { t ->
                t is Class<*> && t == Number::class.java
            })
            Assertions.assertTrue(cache.keys.any { t ->
                t is ParameterizedType
                        && t.rawType == List::class.java
                        && t.actualTypeArguments.size == 1
                        && t.actualTypeArguments.first() == Number::class.java
            })
            Assertions.assertTrue(cache.keys.any { t ->
                t is GenericArrayType
                        && t.typeName == "T[]"
            })
        }
    }

    @Test
    fun `arrays with generics can be resolved`() {
        runBlockingWithContext {
            val cache = HashMap<Type, FuzzedType>()
            val methods = Stubs::class.java.methods
            val method = methods.first { it.name == "arrayLength" }
            method.genericParameterTypes.map {
                toFuzzerType(it, cache)
            }
            Assertions.assertEquals(
                4,
                cache.size
            ) { "Cache should contain following types: List<Number>, Number and T[] for $method" }
            Assertions.assertTrue(cache.keys.any { t ->
                t is Class<*> && t == Number::class.java
            })
            Assertions.assertTrue(cache.keys.any { t ->
                t is ParameterizedType
                        && t.rawType == List::class.java
                        && t.actualTypeArguments.size == 1
                        && t.actualTypeArguments.first().typeName == "T"
            })
            Assertions.assertTrue(cache.keys.any { t ->
                t is GenericArrayType
                        && t.typeName == "java.util.List<T>[]"
            })
            Assertions.assertTrue(cache.keys.any { t ->
                t is GenericArrayType
                        && t.typeName == "java.util.List<T>[][]"
            })
        }
    }

    @Test
    fun `run complex type dependency call`() {
        runBlockingWithContext {
            val cache = HashMap<Type, FuzzedType>()
            val methods = Stubs::class.java.methods
            val method = methods.first { it.name == "example" }
            val types = method.genericParameterTypes
            Assertions.assertTrue(types.size == 3 && types[0].typeName == "A" && types[1].typeName == "B" && types[2].typeName == "C") { "bad input parameters" }
            method.genericParameterTypes.map {
                toFuzzerType(it, cache)
            }
            Assertions.assertEquals(4, cache.size)
            val typeIterableB = cache[types[0].replaceWithUpperBoundUntilNotTypeVariable()]!!
            val genericOfIterableB = with(typeIterableB) {
                Assertions.assertEquals(iterableClassId, classId)
                Assertions.assertEquals(1, generics.size)
                generics[0]
            }
            val typeListA = cache[types[1].replaceWithUpperBoundUntilNotTypeVariable()]!!
            val genericOfListA = with(typeListA) {
                Assertions.assertEquals(List::class.id, classId)
                Assertions.assertEquals(1, generics.size)
                generics[0]
            }
            Assertions.assertEquals(1, genericOfIterableB.generics.size)
            Assertions.assertEquals(1, genericOfListA.generics.size)
            Assertions.assertTrue(genericOfIterableB.generics[0] === typeIterableB) { "Because of recursive types generic of B must depend on B itself" }
            Assertions.assertTrue(genericOfListA.generics[0] === typeListA) { "Because of recursive types generic of A must depend on A itself" }

            val typeListC = cache[types[2].replaceWithUpperBoundUntilNotTypeVariable()]!!
            val genericOfListC = with(typeListC) {
                Assertions.assertEquals(List::class.id, classId)
                Assertions.assertEquals(1, generics.size)
                generics[0]
            }

            Assertions.assertEquals(1, genericOfListC.generics.size)
            Assertions.assertEquals(iterableClassId, genericOfListC.generics[0].classId)
            Assertions.assertTrue(genericOfListC.generics[0].generics[0] === typeListA) { "Generic of C must lead to type A" }
        }
    }

    @Test
    fun `can correctly gather hierarchy information`() {
        runBlockingWithContext {
            val cache = HashMap<Type, FuzzedType>()
            val methods = Implementations::class.java.methods
            val method = methods.first { it.name == "test" }
            val type = method.genericParameterTypes.map {
                toFuzzerType(it, cache)
            }.first()

            val badType = toFuzzerType(Implementations.AString::class.java, cache)
            val badTypeHierarchy = badType.traverseHierarchy(cache).toSet()
            Assertions.assertEquals(2, badTypeHierarchy.size) { "There's only one class (Object) and one interface should be found" }
            Assertions.assertFalse(badTypeHierarchy.contains(type)) { "Bad type hierarchy should not contain tested type $type" }

            val goodType = toFuzzerType(Implementations.AInteger::class.java, cache)
            val goodTypeHierarchy = goodType.traverseHierarchy(cache).toSet()
            Assertions.assertEquals(2, goodTypeHierarchy.size) { "There's only one class (Object) and one interface should be found" }
            Assertions.assertTrue(goodTypeHierarchy.contains(type)) { "Good type hierarchy should contain tested type $type" }
        }
    }
}