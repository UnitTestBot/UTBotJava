package org.utbot.fuzzing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.utbot.framework.fuzzer.IdentityPreservingIdGenerator
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.constructor.ValueConstructor
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzing.providers.NullValueProvider
import org.utbot.fuzzing.samples.AccessibleObjects
import org.utbot.fuzzing.samples.DeepNested
import org.utbot.fuzzing.samples.FailToGenerateListGeneric
import org.utbot.fuzzing.samples.StringListHolder
import org.utbot.fuzzing.samples.Stubs
import org.utbot.fuzzing.utils.Trie
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.jvm.javaMethod

internal object TestIdentityPreservingIdGenerator : IdentityPreservingIdGenerator<Int> {
    private val cache = mutableMapOf<Any, Int>()
    private val gen = AtomicInteger()
    override fun getOrCreateIdForValue(value: Any): Int = cache.computeIfAbsent(value) { createId() }
    override fun createId(): Int = gen.incrementAndGet()
}

class JavaFuzzingTest {

    @Test
    fun `fuzzing doesn't throw an exception when type is unknown`() {
        var count = 0
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
                count += 1
                BaseFeedback(Trie.emptyNode(), Control.STOP)
            }
        }
        assertEquals(0, count)
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
    fun `fuzzing should not generate values of private classes`() {
        var exec = 0
        runBlockingWithContext {
            runJavaFuzzing(
                TestIdentityPreservingIdGenerator,
                methodUnderTest = AccessibleObjects::class.java.declaredMethods.first { it.name == "test" }.executableId,
                constants = emptyList(),
                names = emptyList(),
            ) { _, _, v ->
                if (v.first().model !is UtNullModel) {
                    exec += 1
                }
                BaseFeedback(Trie.emptyNode(), Control.STOP)
            }
        }
        assertEquals(0, exec) { "Fuzzer should not create any values of private classes" }
    }

    @Test
    fun `fuzzing should not generate values of private enums`() {
        var exec = 0
        runBlockingWithContext {
            runJavaFuzzing(
                TestIdentityPreservingIdGenerator,
                methodUnderTest = AccessibleObjects::class.java.declaredMethods.first { it.name == "ordinal" }.executableId,
                constants = emptyList(),
                names = emptyList(),
            ) { _, _, v ->
                if (v.first().model !is UtNullModel) {
                    exec += 1
                }
                BaseFeedback(Trie.emptyNode(), Control.STOP)
            }
        }
        assertEquals(0, exec) { "Fuzzer should not create any values of private classes" }
    }

    @Test
    fun `fuzzing generate single test in case of collection with fail-to-generate generic type`() {
        val size = 100
        var exec = size
        val collections = ArrayList<Any?>(exec)
        runBlockingWithContext {
            runJavaFuzzing(
                TestIdentityPreservingIdGenerator,
                methodUnderTest = FailToGenerateListGeneric::class.java.declaredMethods.first { it.name == "func" }.executableId,
                constants = emptyList(),
                names = emptyList(),
                providers = defaultValueProviders(TestIdentityPreservingIdGenerator).map { it.except { it is NullValueProvider } }
            ) { _, _, v ->
                collections.add(v.first().model as? UtAssembleModel)
                BaseFeedback(Trie.emptyNode(), if (--exec > 0) Control.CONTINUE else Control.STOP)
            }
        }
        assertEquals(0, exec) { "Total fuzzer run number must be 0" }
        assertEquals(size, collections.size) { "Total generated values number must be $size" }
        assertEquals(size, collections.count { it is UtAssembleModel }) { "Total assemble models size must be $size" }
        collections.filterIsInstance<UtAssembleModel>().forEach {
            assertEquals(0, it.modificationsChain.size)
        }
    }

    @Test
    fun `fuzzer correctly works with settable field that has a parameterized type`() {
        val seenStringListHolders = mutableListOf<StringListHolder>()
        var remainingRuns = 100
        runBlockingWithContext {
            runJavaFuzzing(
                TestIdentityPreservingIdGenerator,
                methodUnderTest = StringListHolder::methodUnderTest.javaMethod!!.executableId,
                constants = emptyList(),
                names = emptyList(),
            ) { thisInstance, _, _ ->
                thisInstance?.let {
                    seenStringListHolders.add(
                        ValueConstructor().construct(listOf(it.model)).single().value as StringListHolder
                    )
                }
                remainingRuns--
                BaseFeedback(Trie.emptyNode(), if (remainingRuns > 0) Control.CONTINUE else Control.STOP)
            }
        }
        val seenStrings = seenStringListHolders.flatMap { it.strings.orEmpty().filterNotNull() }
        assertNotEquals(emptyList<String>(), seenStrings)
        seenStrings.forEach { assertInstanceOf(String::class.java, it) }
    }

    @Test
    fun `value providers override every function of fuzzing in simple case`() {
        val provided = MarkerValueProvider<FuzzedType, FuzzedValue, FuzzedDescription>("p")
        `value providers override every function of fuzzing`(provided, provided)
    }

    @Test
    fun `value providers override every function of fuzzing when merging`() {
        val provider1 = MarkerValueProvider<FuzzedType, FuzzedValue, FuzzedDescription>("p1")
        val provider2 = MarkerValueProvider<FuzzedType, FuzzedValue, FuzzedDescription>("p2")
        val provided = provider1.with(provider2)
        `value providers override every function of fuzzing`(provided, provider1)
        `value providers override every function of fuzzing`(provided, provider2)
    }

    @Test
    fun `value providers override every function of fuzzing when excepting`() {
        val provider1 = MarkerValueProvider<FuzzedType, FuzzedValue, FuzzedDescription>("p1")
        val provider2 = MarkerValueProvider<FuzzedType, FuzzedValue, FuzzedDescription>("p2")
        val provided = provider1.except(provider2)
        `value providers override every function of fuzzing`(provided, provider1)
    }

    @Test
    fun `value providers override every function of fuzzing when fallback`() {
        val provider1 = MarkerValueProvider<FuzzedType, FuzzedValue, FuzzedDescription>("p1")
        val provider2 = MarkerValueProvider<FuzzedType, FuzzedValue, FuzzedDescription>("p2")
        val provided = provider1.withFallback(provider2)
        `value providers override every function of fuzzing`(provided, provider1)
        `value providers override every function of fuzzing`(provided, provider2)
    }

    private fun `value providers override every function of fuzzing`(provided: JavaValueProvider, valueProvider: MarkerValueProvider<FuzzedType, FuzzedValue, FuzzedDescription>) {
        var executions = 0
        runBlockingWithContext {
            runJavaFuzzing(
                TestIdentityPreservingIdGenerator,
                methodUnderTest = FailToGenerateListGeneric::class.java.declaredMethods.first { it.name == "func" }.executableId,
                constants = emptyList(),
                names = emptyList(),
                providers = listOfNotNull(provided)
            ) { _, _, _ ->
                executions++
                BaseFeedback(Trie.emptyNode(), Control.STOP)
            }
        }

        assertNotEquals(0, valueProvider.enrich) { "Enrich is never called for ${valueProvider.name}" }
        assertNotEquals(0, valueProvider.accept) { "Accept is never called for ${valueProvider.name}" }
        assertNotEquals(0, valueProvider.generate) { "Generate is never called for ${valueProvider.name}" }
        assertEquals(0, executions) { "Execution must be never called, because of empty seed supply for ${valueProvider.name}" }
    }
}

class MarkerValueProvider<T, R, D : Description<T>>(
    val name: String
) : ValueProvider<T, R, D> {
    var enrich: Int = 0
    var accept: Int = 0
    var generate: Int = 0

    override fun enrich(description: D, type: T, scope: Scope) {
        enrich++
    }

    override fun accept(type: T): Boolean {
        accept++
        return true
    }

    override fun generate(description: D, type: T): Sequence<Seed<T, R>> {
        generate++
        return emptySequence()
    }
}