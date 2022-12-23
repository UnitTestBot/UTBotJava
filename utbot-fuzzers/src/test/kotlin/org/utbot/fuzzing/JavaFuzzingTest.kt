package org.utbot.fuzzing

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.TestIdentityPreservingIdGenerator
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzing.samples.DeepNested
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzing.samples.Stubs
import org.utbot.fuzzing.utils.Trie
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.IdentityHashMap

class JavaFuzzingTest {

    @Test
    fun `fuzzing doesn't throw an exception when type is unknown`() {
        assertDoesNotThrow {
            runBlockingWithContext {
                runJavaFuzzing(
                    TestIdentityPreservingIdGenerator,
                    methodUnderTest = MethodId(
                        DeepNested.Nested1.Nested2::class.id,
                        "f",
                        intClassId,
                        listOf(intClassId)
                    ),
                    constants = emptyList(),
                    names = emptyList(),
                ) { _, _, _ ->
                    fail("This method is never called")
                }
            }
        }
    }

    @Test
    fun `string generates same values`() {
        fun collect(): List<String> {
            return runBlockingWithContext {
                val results = mutableListOf<String>()
                var count = 0
                val probes = 10000
                runJavaFuzzing(
                    TestIdentityPreservingIdGenerator,
                    methodUnderTest = MethodId(Stubs::class.id, "name", voidClassId, listOf(stringClassId)),
                    constants = listOf(
                        FuzzedConcreteValue(stringClassId, "Hello"),
                        FuzzedConcreteValue(stringClassId, "World"),
                        FuzzedConcreteValue(stringClassId, "!"),
                    ),
                    names = emptyList()
                ) { _, _, values ->
                    results += (values.first().model as UtPrimitiveModel).value as String
                    BaseFeedback(Trie.emptyNode(), if (++count < probes) Control.CONTINUE else Control.STOP)
                }
                assertEquals(count, results.size)
                results
            }
        }

        val probe1 = collect()
        val probe2 = collect()
        assertEquals(probe1, probe2)
    }

    @Test
    fun `recursive generic types are recognized correctly`() {
        runBlockingWithContext {
            val methods = Stubs::class.java.methods
            val method = methods.first { it.name == "resolve" && it.returnType == Int::class.javaPrimitiveType }
            val typeCache = IdentityHashMap<Type, FuzzedType>()
            val type = toFuzzerType(method.genericParameterTypes.first(), typeCache)
            assertEquals(1, typeCache.size)
            assertTrue(typeCache.values.all { type === it })
            assertEquals(1, type.generics.size)
            assertTrue(typeCache.values.all { type.generics[0] === it })

            try {
                // If FuzzerType has implemented `equals` and `hashCode` or is data class,
                // that implements those methods implicitly,
                // then adding it to hash table throws [StackOverflowError]
                val set = HashSet<FuzzedType>()
                set += type
            } catch (soe: StackOverflowError) {
                fail("Looks like FuzzerType implements equals and hashCode, " +
                        "which leads unstable behaviour in recursive generics ", soe)
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
            assertEquals(3, cache.size) { "Cache should contain following types: List<Number>, Number and T[] for $method" }
            assertTrue(cache.keys.any { t ->
                t is Class<*> && t == java.lang.Number::class.java
            })
            assertTrue(cache.keys.any { t ->
                t is ParameterizedType
                        && t.rawType == java.util.List::class.java
                        && t.actualTypeArguments.size == 1
                        && t.actualTypeArguments.first() == java.lang.Number::class.java
            })
            assertTrue(cache.keys.any { t ->
                t is GenericArrayType
                        && t.typeName == "T[]"
            })
        }
    }

    @Test
    fun `arrays with generics can be resolved`() {
        runBlockingWithContext {
            val cache = IdentityHashMap<Type, FuzzedType>()
            val methods = Stubs::class.java.methods
            val method = methods.first { it.name == "arrayLength" }
            method.genericParameterTypes.map {
                toFuzzerType(it, cache)
            }
            assertEquals(4, cache.size) { "Cache should contain following types: List<Number>, Number and T[] for $method" }
            assertTrue(cache.keys.any { t ->
                t is Class<*> && t == java.lang.Number::class.java
            })
            assertTrue(cache.keys.any { t ->
                t is ParameterizedType
                        && t.rawType == java.util.List::class.java
                        && t.actualTypeArguments.size == 1
                        && t.actualTypeArguments.first().typeName == "T"
            })
            assertTrue(cache.keys.any { t ->
                t is GenericArrayType
                        && t.typeName == "java.util.List<T>[]"
            })
            assertTrue(cache.keys.any { t ->
                t is GenericArrayType
                        && t.typeName == "java.util.List<T>[][]"
            })
        }
    }

    private fun <T> runBlockingWithContext(block: suspend () -> T) : T {
        return withUtContext(UtContext(this::class.java.classLoader)) {
            runBlocking {
                block()
            }
        }
    }
}