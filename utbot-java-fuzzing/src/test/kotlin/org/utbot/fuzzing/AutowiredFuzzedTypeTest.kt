package org.utbot.fuzzing

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isRefType
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.providers.*
import org.utbot.fuzzing.samples.Types
import org.utbot.fuzzing.type.factories.SimpleFuzzedTypeFactory
import org.utbot.fuzzing.type.factories.SpringFuzzedTypeFactory
import org.utbot.fuzzing.utils.Trie

class AutowiredFuzzedTypeTest {

    @Test
    fun `test simple type generator has the cache`() {
        val factory = SimpleFuzzedTypeFactory()
        val intType = factory.createFuzzedType(Integer::class.java, false)
        val doubleType = factory.createFuzzedType(Double::class.java, false)
        val booleanType = factory.createFuzzedType(Boolean::class.java, false)
        val objectType = factory.createFuzzedType(Any::class.java, false)
        val recursiveType = factory.createFuzzedType(Types.MyBean::class.java, false)
        repeat(10000) {
            Assertions.assertTrue(intType === factory.createFuzzedType(Integer::class.java, false))
            Assertions.assertTrue(doubleType === factory.createFuzzedType(Double::class.java, false))
            Assertions.assertTrue(booleanType === factory.createFuzzedType(Boolean::class.java, false))
            Assertions.assertTrue(objectType === factory.createFuzzedType(Any::class.java, false))
            Assertions.assertTrue(recursiveType === factory.createFuzzedType(Types.MyBean::class.java, false))
        }
    }

    @Test
    @Suppress("MoveLambdaOutsideParentheses")
    fun `test spring type generator has the cache`() {
        val factory = SpringFuzzedTypeFactory(
            SimpleFuzzedTypeFactory(),
            AutowiredValueProvider(TestIdentityPreservingIdGenerator) { beanName ->
                when (beanName) {
                    "Test" -> UtModel(voidClassId)
                    else -> error("no way")
                }
            },
            { classId ->
                if (classId.name.replace("$", ".") == Types.MyBean::class.java.canonicalName) {
                    listOf("Test")
                } else {
                    emptyList()
                }
            }
        )
        val intType = factory.createFuzzedType(Integer::class.java, true)
        val doubleType = factory.createFuzzedType(Double::class.java, true)
        val booleanType = factory.createFuzzedType(Boolean::class.java, true)
        val objectType = factory.createFuzzedType(Any::class.java, true)
        val recursiveType = factory.createFuzzedType(Types.MyBean::class.java, true)
        repeat(10000) {
            Assertions.assertTrue(intType === factory.createFuzzedType(Integer::class.java, true))
            Assertions.assertTrue(doubleType === factory.createFuzzedType(Double::class.java, true))
            Assertions.assertTrue(booleanType === factory.createFuzzedType(Boolean::class.java, true))
            Assertions.assertTrue(objectType === factory.createFuzzedType(Any::class.java, true))
            Assertions.assertTrue(recursiveType === factory.createFuzzedType(Types.MyBean::class.java, true))
        }
    }

    @Suppress("MoveLambdaOutsideParentheses")
    private fun `test MyBean example template`(mutSupplier: () -> MethodId) {
        val autowiredValueProvider = AutowiredValueProvider(TestIdentityPreservingIdGenerator) { beanName ->
            when (beanName) {
                "Test" -> UtModel(voidClassId)
                else -> error("no way")
            }
        }
        val factory = SpringFuzzedTypeFactory(
            SimpleFuzzedTypeFactory(),
            autowiredValueProvider,
            { classId ->
                if (classId.name.replace("$", ".") == Types.MyBean::class.java.canonicalName) {
                    listOf("Test")
                } else {
                    emptyList()
                }
            }
        )


        runBlockingWithContext {
            var attempts = 1000
            runJavaFuzzing(
                idGenerator = TestIdentityPreservingIdGenerator,
                methodUnderTest = mutSupplier.invoke(),
                constants = emptyList(),
                names = emptyList(),
                providers = listOf(
                    DelegatingToCustomJavaValueProvider,
                    ValueProvider { _, type ->
                        sequence {
                            if (type.classId.isRefType) {
                                yield(Seed.Simple(UtModel(voidClassId).fuzzed()))
                            }
                        }
                    },
                    IntegerValueProvider
                ),
                fuzzedTypeFactory = factory,
                exec = { _, _, _ ->
                    val control = if (--attempts >= 0) Control.CONTINUE else Control.STOP
                    BaseFeedback(Trie.emptyNode(), control)
                }
            )
        }
    }

    @Test
    fun `spring application doesn't exceed memory when fuzzing this instance`() {
        `test MyBean example template` {
            val beanId = Types.MyBean::class.java.id
            MethodId(beanId, "foo", voidClassId, listOf(intClassId)) }
    }

    @Test
    fun `spring application doesn't exceed memory when fuzzing custom instances`() {
        `test MyBean example template` {
            val beanId = Types.MyBean::class.java.id
            MethodId(Types::class.java.id, "bar", voidClassId, listOf(beanId)) }
    }
}