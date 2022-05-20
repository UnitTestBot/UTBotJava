package org.utbot.framework.plugin.api

import org.utbot.fuzzer.CartesianProduct
import org.utbot.fuzzer.Combinations
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.math.pow

class CombinationsTest {

    @Test
    fun testSpecification() {
        val combinations = Combinations(2, 3)
        assertArrayEquals(intArrayOf(0, 0), combinations[0])
        assertArrayEquals(intArrayOf(0, 1), combinations[1])
        assertArrayEquals(intArrayOf(0, 2), combinations[2])
        assertArrayEquals(intArrayOf(1, 0), combinations[3])
        assertArrayEquals(intArrayOf(1, 1), combinations[4])
        assertArrayEquals(intArrayOf(1, 2), combinations[5])
    }

    @Test
    fun testCartesianProduct() {
        val result: List<List<*>> = CartesianProduct(
            listOf(
                listOf("x", "y", "z"),
                listOf(1.0, 2.0, 3.0)
            )
        ).toList()
        assertEquals(9, result.size)
        assertEquals(listOf("x", 1.0), result[0])
        assertEquals(listOf("x", 2.0), result[1])
        assertEquals(listOf("x", 3.0), result[2])
        assertEquals(listOf("y", 1.0), result[3])
        assertEquals(listOf("y", 2.0), result[4])
        assertEquals(listOf("y", 3.0), result[5])
        assertEquals(listOf("z", 1.0), result[6])
        assertEquals(listOf("z", 2.0), result[7])
        assertEquals(listOf("z", 3.0), result[8])
    }

    @Test
    fun testCombinationsIsLazy() {
        val combinations = Combinations(2, 2, 2)
        val taken = combinations.take(3)
        assertArrayEquals(intArrayOf(0, 0, 0), taken[0])
        assertArrayEquals(intArrayOf(0, 0, 1), taken[1])
        assertArrayEquals(intArrayOf(0, 1, 0), taken[2])
    }

    @Test
    fun testDecimalNumbers() {
        val array = intArrayOf(10, 10, 10)
        val combinations = Combinations(*array)
        combinations.forEachIndexed { i, c ->
            var actual = 0
            for (pos in array.indices) {
                actual += c[pos] * (10.0.pow(array.size - 1.0 - pos).toInt())
            }
            assertEquals(i, actual)
        }
    }

    @Test
    fun testDecimalNumbersByString() {
        Combinations(10, 10, 10).forEachIndexed { number, digits ->
            assertEquals(String.format("%03d", number), digits.joinToString(separator = "") { it.toString() })
        }
    }

    @Test
    fun testSomeNumbers() {
        val c = Combinations(3, 4, 2)
        assertArrayEquals(intArrayOf(0, 3, 0), c[6])
        assertArrayEquals(intArrayOf(1, 0, 1), c[9])
    }

    @Test
    fun testZeroValues() {
        val c = Combinations()
        assertEquals(0, c.size)
    }

    @Test
    fun testZeroValuesWithException() {
        val c = Combinations()
        assertThrows(IllegalArgumentException::class.java) {
            @Suppress("UNUSED_VARIABLE") val result = c[0]
        }
    }

    @Test
    fun testZeroAsMaxValues() {
        assertThrows(java.lang.IllegalArgumentException::class.java) {
            Combinations(0, 0, 0)
        }
    }

    @Test
    fun testZeroInTheMiddle() {
        assertThrows(IllegalArgumentException::class.java) {
            Combinations(2, -1, 3)
        }
    }

}