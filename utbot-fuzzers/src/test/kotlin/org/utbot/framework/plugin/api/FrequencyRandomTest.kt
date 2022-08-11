package org.utbot.framework.plugin.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.utbot.fuzzer.FrequencyRandom
import kotlin.math.abs
import kotlin.random.Random

class FrequencyRandomTest {

    @Test
    fun `default implementation always returns 0`() {
        val random = FrequencyRandom()
        (0 until 1000).forEach { _ ->
            assertEquals(0, random.nextIndex())
        }
    }

    @ParameterizedTest(name = "seed{arguments}")
    @ValueSource(ints = [0, 100, -123, 99999, 84])
    fun `with default forEach function frequencies is equal`(seed: Int) {
        val random = FrequencyRandom(Random(seed))
        val frequencies = listOf(10, 20, 30, 40)
        val result = IntArray(frequencies.size)
        random.prepare(frequencies)
        assertEquals(100, frequencies.sum()) { "In this test every frequency value represents a percent. The sum must be equal to 100" }
        assertEquals(frequencies.size, random.size)
        val tries = 100_000
        val errors = tries / 100 // 1%
        (0 until tries).forEach { _ ->
            result[random.nextIndex()]++
        }
        val expected = frequencies.map { tries * it / 100}
        result.forEachIndexed { index, value ->
            assertTrue(abs(expected[index] - value) < errors) {
                "The error should not extent $errors for $tries cases, but ${expected[index]} and $value too far"
            }
        }
    }

    @Test
    fun `inverting probabilities from the documentation`() {
        val frequencies = listOf(20.0, 80.0)
        val random = FrequencyRandom(Random(0)).apply {
            prepare(frequencies) { 100.0 - it }
        }
        val result = IntArray(frequencies.size)
        val tries = 10_000
        val errors = tries / 100 // 1%
        (0 until tries).forEach { _ ->
            result[random.nextIndex()]++
        }
        result.forEachIndexed { index, value ->
            val expected = frequencies[frequencies.size - 1 - index] * errors
            assertTrue(abs(value - expected) < errors) {
                "The error should not extent 100 for 10 000 cases, but $expected and $value too far"
            }
        }
    }
}