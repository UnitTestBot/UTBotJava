package org.utbot.framework.plugin.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.utbot.fuzzer.chooseOne
import org.utbot.fuzzer.flipCoin
import kotlin.math.abs
import kotlin.random.Random

class RandomExtensionsTest {

    @Test
    fun `default implementation always returns 0`() {
        val frequencies = doubleArrayOf(1.0)
        (0 until 1000).forEach { _ ->
            assertEquals(0, Random.chooseOne(frequencies))
        }
    }

    @ParameterizedTest(name = "seed{arguments}")
    @ValueSource(ints = [0, 100, -123, 99999, 84])
    fun `with default forEach function frequencies is equal`(seed: Int) {
        val random = Random(seed)
        val frequencies = doubleArrayOf(10.0, 20.0, 30.0, 40.0)
        val result = IntArray(frequencies.size)
        assertEquals(100.0, frequencies.sum()) { "In this test every frequency value represents a percent. The sum must be equal to 100" }
        val tries = 100_000
        val errors = tries / 100 // 1%
        (0 until tries).forEach { _ ->
            result[random.chooseOne(frequencies)]++
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
        val frequencies = doubleArrayOf(20.0, 80.0)
        val random = Random(0)
        val result = IntArray(frequencies.size)
        val tries = 10_000
        val errors = tries / 100 // 1%
        (0 until tries).forEach { _ ->
            result[random.chooseOne(DoubleArray(frequencies.size) { 100.0 - frequencies[it] })]++
        }
        result.forEachIndexed { index, value ->
            val expected = frequencies[frequencies.size - 1 - index] * errors
            assertTrue(abs(value - expected) < errors) {
                "The error should not extent 100 for 10 000 cases, but $expected and $value too far"
            }
        }
    }

    @Test
    fun `flip the coin is fair enough`() {
        val random = Random(0)
        var result = 0
        val probability = 20
        val experiments = 1_000_000
        for (i in 0 until experiments) {
            if (random.flipCoin(probability)) {
                result++
            }
        }
        val error = experiments / 1000 // 0.1 %
        assertTrue(abs(result - experiments * probability / 100) < error)
    }
}