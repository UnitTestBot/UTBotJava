package org.utbot.fuzzing.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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

    @Test
    fun `invert bit works for long`() {
        var attempts = 100_000
        val random = Random(2210)
        sequence {
            while (true) {
                yield(random.nextLong())
            }
        }.forEach { value ->
            if (attempts-- <= 0) { return }
            for (bit in 0 until Long.SIZE_BITS) {
                val newValue = value.invertBit(bit)
                val oldBinary = value.toBinaryString()
                val newBinary = newValue.toBinaryString()
                assertEquals(oldBinary.length, newBinary.length)
                for (test in Long.SIZE_BITS - 1 downTo 0) {
                    if (test != Long.SIZE_BITS - 1 - bit) {
                        assertEquals(oldBinary[test], newBinary[test]) { "$oldBinary : $newBinary for value $value" }
                    } else {
                        assertNotEquals(oldBinary[test], newBinary[test]) { "$oldBinary : $newBinary for value $value" }
                    }
                }
            }
        }
    }

    private fun Long.toBinaryString() = java.lang.Long.toBinaryString(this).padStart(Long.SIZE_BITS, '0')
}