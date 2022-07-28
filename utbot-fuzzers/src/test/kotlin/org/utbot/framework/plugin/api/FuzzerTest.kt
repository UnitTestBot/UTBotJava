package org.utbot.framework.plugin.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.fuzz
import org.utbot.fuzzer.providers.ConstantsModelProvider
import org.utbot.fuzzer.providers.NullModelProvider
import org.utbot.fuzzer.providers.PrimitiveDefaultsModelProvider
import org.utbot.fuzzer.providers.PrimitiveWrapperModelProvider.fuzzed
import java.lang.IllegalArgumentException
import java.util.concurrent.TimeUnit

class FuzzerTest {

    private val testProvider = ModelProvider.of(PrimitiveDefaultsModelProvider, NullModelProvider)

    @Test
    fun `error when no provider is passed`() {
        assertThrows<IllegalArgumentException> {
            fuzz(newDescription(emptyList()))
        }
    }

    @Test
    fun `zero values for empty input`() {
        val fuzz = fuzz(newDescription(emptyList()), testProvider)
        assertNull(fuzz.firstOrNull())
    }

    @Test
    fun `single value for every type`() {
        val fuzz = fuzz(
            newDescription(
                defaultTypes()
            ),
            testProvider
        )
        assertEquals(1, fuzz.count()) { "Default provider should create 1 value for every type, but have ${fuzz.count()}" }
        assertEquals(listOf(
            UtPrimitiveModel(false),
            UtPrimitiveModel(0.toByte()),
            UtPrimitiveModel('\u0000'),
            UtPrimitiveModel(0.toShort()),
            UtPrimitiveModel(0),
            UtPrimitiveModel(0L),
            UtPrimitiveModel(0.0f),
            UtPrimitiveModel(0.0),
            UtNullModel(Any::class.java.id)
        ), fuzz.first().map { it.model })
    }

    @Test
    fun `concrete values are created`() {
        val concreteValues = listOf(
            FuzzedConcreteValue(intClassId, 1),
            FuzzedConcreteValue(intClassId, 2),
            FuzzedConcreteValue(intClassId, 3),
        )
        val fuzz = fuzz(newDescription(listOf(intClassId), concreteValues), ConstantsModelProvider)
        assertEquals(concreteValues.size, fuzz.count())
        assertEquals(setOf(
            UtPrimitiveModel(1),
            UtPrimitiveModel(2),
            UtPrimitiveModel(3),
        ), fuzz.map { it.first().model }.toSet())
    }

    @Test
    fun `concrete values are created but filtered`() {
        val concreteValues = listOf(
            FuzzedConcreteValue(intClassId, 1),
            FuzzedConcreteValue(intClassId, 2),
            FuzzedConcreteValue(intClassId, 3),
        )
        val fuzz = fuzz(newDescription(listOf(charClassId), concreteValues), ConstantsModelProvider)
        assertEquals(0, fuzz.count())
    }

    @Test
    fun `all combinations is found`() {
        val fuzz = fuzz(newDescription(listOf(booleanClassId, intClassId)), ModelProvider {
            sequenceOf(
                FuzzedParameter(0, UtPrimitiveModel(true).fuzzed()),
                FuzzedParameter(0, UtPrimitiveModel(false).fuzzed()),
                FuzzedParameter(1, UtPrimitiveModel(-1).fuzzed()),
                FuzzedParameter(1, UtPrimitiveModel(0).fuzzed()),
                FuzzedParameter(1, UtPrimitiveModel(1).fuzzed()),
            )
        })
        assertEquals(6, fuzz.count())
        assertEquals(setOf(
            listOf(UtPrimitiveModel(true), UtPrimitiveModel(-1)),
            listOf(UtPrimitiveModel(false), UtPrimitiveModel(-1)),
            listOf(UtPrimitiveModel(true), UtPrimitiveModel(0)),
            listOf(UtPrimitiveModel(false), UtPrimitiveModel(0)),
            listOf(UtPrimitiveModel(true), UtPrimitiveModel(1)),
            listOf(UtPrimitiveModel(false), UtPrimitiveModel(1)),
        ), fuzz.map { arguments -> arguments.map { fuzzedValue -> fuzzedValue.model } }.toSet())
    }

    // Because of Long limitation fuzzer can process no more than 511 values for method with 7 parameters
    @Test
    @Timeout(1, unit = TimeUnit.SECONDS)
    fun `the worst case works well`() {
        assertDoesNotThrow {
            val values = (0 until 511).map { UtPrimitiveModel(it).fuzzed() }.asSequence()
            val provider = ModelProvider { descr ->
                (0 until descr.parameters.size).asSequence()
                    .flatMap { index -> values.map { FuzzedParameter(index, it) } }
            }
            val parameters = (0 until 7).mapTo(mutableListOf()) { intClassId }
            val fuzz = fuzz(newDescription(parameters), provider)
            val first10 = fuzz.take(10).toList()
            assertEquals(10, first10.size)
        }
    }

    private fun defaultTypes(includeStringId: Boolean = false): List<ClassId> {
        val result = mutableListOf(
            booleanClassId,
            byteClassId,
            charClassId,
            shortClassId,
            intClassId,
            longClassId,
            floatClassId,
            doubleClassId,
        )
        if (includeStringId) {
            result += stringClassId
        }
        result += Any::class.java.id
        return result
    }

    private fun newDescription(
        parameters: List<ClassId>,
        concreteValues: Collection<FuzzedConcreteValue> = emptyList()
    ): FuzzedMethodDescription {
        return FuzzedMethodDescription("testMethod", voidClassId, parameters, concreteValues)
    }

}