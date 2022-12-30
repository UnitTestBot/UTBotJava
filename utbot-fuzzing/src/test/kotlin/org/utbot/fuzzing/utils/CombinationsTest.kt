package org.utbot.fuzzing.utils

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.BitSet
import kotlin.math.pow
import kotlin.random.Random

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
            var actual = 0L
            for (pos in array.indices) {
                actual += c[pos] * (10.0.pow(array.size - 1.0 - pos).toInt())
            }
            assertEquals(i.toLong(), actual)
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

    @ParameterizedTest(name = "testAllLongValues{arguments}")
    @ValueSource(ints = [1, 100, Int.MAX_VALUE])
    fun testAllLongValues(value: Int) {
        val combinations = Combinations(value, value, 2)
        assertEquals(2L * value * value, combinations.size)
        val array = combinations[combinations.size - 1]
        assertEquals(value - 1, array[0])
        assertEquals(value - 1, array[1])
        assertEquals(1, array[2])
    }

    @Test
    fun testCartesianFindsAllValues() {
        val radix = 4
        val product = createIntCartesianProduct(radix, 10)
        val total = product.estimatedSize
        assertTrue(total < Int.MAX_VALUE) { "This test should generate less than Int.MAX_VALUE values but has $total" }

        val set = BitSet((total / 64).toInt())
        val updateSet: (List<String>) -> Unit = {
            val value = it.joinToString("").toLong(radix).toInt()
            assertFalse(set[value])
            set.set(value)
        }
        val realCount = product.onEach(updateSet).count()
        assertEquals(total, realCount.toLong())

        for (i in 0 until total) {
            assertTrue(set[i.toInt()]) { "Values is not listed for index = $i" }
        }
        for (i in total until set.size()) {
            assertFalse(set[i.toInt()])
        }
    }

    /**
     * Creates all numbers from 0 to `radix^repeat`.
     *
     * For example:
     *
     * radix = 2, repeat = 2 -> {'0', '0'}, {'0', '1'}, {'1', '0'}, {'1', '1'}
     * radix = 16, repeat = 1 -> {'0'}, {'1'}, {'2'}, {'3'}, {'4'}, {'5'}, {'6'}, {'7'}, {'8'}, {'9'}, {'a'}, {'b'}, {'c'}, {'d'}, {'e'}, {'f'}
     */
    private fun createIntCartesianProduct(radix: Int, repeat: Int) =
        CartesianProduct(
            lists = (1..repeat).map {
                Array(radix) { it.toString(radix) }.toList()
            },
            random = Random(0)
        ).apply {
            assertEquals((1L..repeat).fold(1L) { acc, _ -> acc * radix }, estimatedSize)
        }

    @Test
    fun testCanCreateCartesianProductWithSizeGreaterThanMaxInt() {
        val product = createIntCartesianProduct(5, 15)
        assertTrue(product.estimatedSize > Int.MAX_VALUE) { "This test should generate more than Int.MAX_VALUE values but has ${product.estimatedSize}" }
        assertDoesNotThrow {
            product.first()
        }
    }

    @Test
    fun testIterationWithChunksIsCorrect() {
        val expected = mutableListOf(
            Triple(0L, 5, 7L),
            Triple(5L, 5, 2L),
            Triple(10L, 2, 0L),
        )
        CartesianProduct.forEachChunk(5, 12) { start, chunk, remain ->
            assertEquals(expected.removeFirst(), Triple(start, chunk, remain))
        }
        assertTrue(expected.isEmpty())
    }

    @Test
    fun testIterationWithChunksIsCorrectWhenChunkIsIntMax() {
        val total = 12
        val expected = mutableListOf(
            Triple(0L, total, 0L)
        )
        CartesianProduct.forEachChunk(Int.MAX_VALUE, total.toLong()) { start, chunk, remain ->
            assertEquals(expected.removeFirst(), Triple(start, chunk, remain))
        }
        assertTrue(expected.isEmpty())
    }

    @ParameterizedTest(name = "testIterationWithChunksIsCorrectWhenChunkIs{arguments}")
    @ValueSource(ints = [1, 2, 3, 4, 6, 12])
    fun testIterationWithChunksIsCorrectWhenChunk(chunkSize: Int) {
        val total = 12
        assertTrue(total % chunkSize == 0) { "Test requires values that are dividers of the total = $total, but it is not true for $chunkSize" }
        val expected = (0 until total step chunkSize).map { it.toLong() }.map {
            Triple(it, chunkSize, total - it - chunkSize)
        }.toMutableList()
        CartesianProduct.forEachChunk(chunkSize, total.toLong()) { start, chunk, remain ->
            assertEquals(expected.removeFirst(), Triple(start, chunk, remain))
        }
        assertTrue(expected.isEmpty())
    }

    @ParameterizedTest(name = "testIterationsWithChunksThroughLongWithRemainingIs{arguments}")
    @ValueSource(longs = [1L, 200L, 307, Int.MAX_VALUE - 1L, Int.MAX_VALUE.toLong()])
    fun testIterationsWithChunksThroughLongTotal(remaining: Long) {
        val expected = mutableListOf(
            Triple(0L, Int.MAX_VALUE, Int.MAX_VALUE + remaining),
            Triple(Int.MAX_VALUE.toLong(), Int.MAX_VALUE, remaining),
            Triple(Int.MAX_VALUE * 2L, remaining.toInt(), 0L),
        )
        CartesianProduct.forEachChunk(Int.MAX_VALUE, Int.MAX_VALUE * 2L + remaining) { start, chunk, remain ->
            assertEquals(expected.removeFirst(), Triple(start, chunk, remain))
        }
        assertTrue(expected.isEmpty())
    }

    @Test
    fun testCartesianProductDoesNotThrowsExceptionBeforeOverflow() {
        // We assume that a standard method has no more than 7 parameters.
        // In this case every parameter can accept up to 511 values without Long overflow.
        // CartesianProduct throws exception
        val values = Array(511) { it }.toList()
        val parameters = Array(7) { values }.toList()
        assertDoesNotThrow {
            CartesianProduct(parameters, Random(0)).asSequence()
        }
    }

    @Test
    fun testCartesianProductThrowsExceptionOnOverflow() {
        // We assume that a standard method has no more than 7 parameters.
        // In this case every parameter can accept up to 511 values without Long overflow.
        // CartesianProduct throws exception
        val values = Array(512) { it }.toList()
        val parameters = Array(7) { values }.toList()
        assertThrows(TooManyCombinationsException::class.java) {
            CartesianProduct(parameters, Random(0)).asSequence()
        }
    }

    @ParameterizedTest(name = "testCombinationHasValue{arguments}")
    @ValueSource(ints = [1, Int.MAX_VALUE])
    fun testCombinationHasValue(value: Int) {
        val combinations = Combinations(value)
        assertEquals(value.toLong(), combinations.size)
        assertEquals(value - 1, combinations[value - 1L][0])
    }

    @Test
    fun testNoFailWhenMixedValues() {
        val combinations = Combinations(2, Int.MAX_VALUE)
        assertEquals(2 * Int.MAX_VALUE.toLong(), combinations.size)
        assertArrayEquals(intArrayOf(0, 0), combinations[0L])
        assertArrayEquals(intArrayOf(0, Int.MAX_VALUE - 1), combinations[Int.MAX_VALUE - 1L])
        assertArrayEquals(intArrayOf(1, 0), combinations[Int.MAX_VALUE.toLong()])
        assertArrayEquals(intArrayOf(1, 1), combinations[Int.MAX_VALUE + 1L])
        assertArrayEquals(intArrayOf(1, Int.MAX_VALUE - 1), combinations[Int.MAX_VALUE * 2L - 1])
    }

    @Test
    fun testLazyCartesian() {
        val source = listOf(
            (1..10).toList(),
            listOf("a", "b", "c"),
            listOf(true, false)
        )
        val eager = CartesianProduct(source).asSequence().toList()
        val lazy = source.cartesian().toList()
        assertEquals(eager.size, lazy.size)
        for (i in eager.indices) {
            assertEquals(eager[i], lazy[i])
        }
    }

    @Test
    fun testLazyCartesian2() {
        val source = listOf(
            (1..10).toList(),
            listOf("a", "b", "c"),
            listOf(true, false)
        )
        val eager = CartesianProduct(source).asSequence().toList()
        var index = 0
        source.cartesian {
            assertEquals(eager[index++], it)
        }
    }
}