package org.utbot.framework.plugin.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.utbot.fuzzer.PseudoShuffledIntProgression
import kotlin.random.Random

class PseudoShuffledIntProgressionTest {

    @Test
    fun testEmpty() {
        val shuffle = PseudoShuffledIntProgression(0)
        assertEquals(0, shuffle.size)
        assertThrows(IllegalStateException::class.java) {
            shuffle[1]
        }
        assertThrows(IllegalStateException::class.java) {
            shuffle[-1]
        }
    }

    @Test
    fun testBadSize() {
        assertThrows(IllegalStateException::class.java) {
            PseudoShuffledIntProgression(-1)
        }
    }

    @Test
    fun testBadSideAndCorrectSize() {
        assertThrows(IllegalStateException::class.java) {
            PseudoShuffledIntProgression(100, Random) { -it }
        }
    }

    @Test
    fun testSingleValue() {
        val shuffle = PseudoShuffledIntProgression(1)
        assertEquals(0, shuffle[0])
    }

    @Test
    fun testSequent() {
        val shuffle = PseudoShuffledIntProgression(intArrayOf(0), intArrayOf(0, 1, 2, 3), intArrayOf())
        assertEquals(4, shuffle.size)
        val result = shuffle.toArray()
        assertArrayEquals(intArrayOf(0, 1, 2, 3), result)
    }

    @Test
    fun testSquare2() {
        val shuffle = PseudoShuffledIntProgression(intArrayOf(0, 1), intArrayOf(0, 1), intArrayOf())
        assertEquals(4, shuffle.size)
        val result = shuffle.toArray()
        assertArrayEquals(intArrayOf(0, 2, 1, 3), result)
    }

    @Test
    fun testSquare3() {
        val shuffle = PseudoShuffledIntProgression(intArrayOf(0, 1, 2), intArrayOf(0, 1, 2), intArrayOf())
        assertEquals(9, shuffle.size)
        val result = shuffle.toArray()
        assertArrayEquals(intArrayOf(0, 3, 6, 1, 4, 7, 2, 5, 8), result)
    }

    @Test
    fun testSquare2WithTail1() {
        val shuffle = PseudoShuffledIntProgression(intArrayOf(0, 1), intArrayOf(0, 1), intArrayOf(5))
        assertEquals(5, shuffle.size)
        val result = shuffle.toArray()
        assertArrayEquals(intArrayOf(0, 2, 5, 1, 3), result)
    }

    @Test
    fun testSquare2WithTail1Reverse() {
        val shuffle = PseudoShuffledIntProgression(intArrayOf(1, 0), intArrayOf(1, 0), intArrayOf(5))
        assertEquals(5, shuffle.size)
        val result = shuffle.toArray()
        assertArrayEquals(intArrayOf(3, 1, 5, 2, 0), result)
    }

    @Test
    fun testSquare3WithFullTailAndReverseOrder() {
        val shuffle = PseudoShuffledIntProgression(intArrayOf(2, 1, 0), intArrayOf(2, 1, 0), intArrayOf(10, 11, 12))
        assertEquals(12, shuffle.size)
        val result = shuffle.toArray()
        assertArrayEquals(intArrayOf(8, 5, 2, 10, 7, 4, 1, 11, 6, 3, 0, 12), result)
    }

    @Test
    fun test2x6andNoTail() {
        val shuffle = PseudoShuffledIntProgression(intArrayOf(0, 1), intArrayOf(0, 1, 2, 3, 4, 5), intArrayOf())
        assertEquals(12, shuffle.size)
        val result = shuffle.toArray()
        assertArrayEquals(intArrayOf(0, 6, 1, 7, 2, 8, 3, 9, 4, 10, 5, 11), result)
    }

    @Test
    fun test2x6and4Tail() {
        val shuffle = PseudoShuffledIntProgression(intArrayOf(0, 1), intArrayOf(0, 1, 2, 3, 4, 5), intArrayOf(20, 21, 22, 23))
        assertEquals(16, shuffle.size)
        val result = shuffle.toArray()
        assertArrayEquals(intArrayOf(0, 6, 20, 1, 7, 21, 2, 8, 22, 3, 9, 23, 4, 10, 5, 11), result)
    }

    @Test
    fun test6x2andNoTail() {
        val shuffle = PseudoShuffledIntProgression(intArrayOf(0, 1, 2, 3, 4, 5), intArrayOf(0, 1), intArrayOf())
        assertEquals(12, shuffle.size)
        val result = shuffle.toArray()
        assertArrayEquals(intArrayOf(0, 2, 4, 6, 8, 10, 1, 3, 5, 7, 9, 11), result)
    }

    @Test
    fun test6x2and4TailFailsBecauseItIsForbiddenSituation() {
        assertThrows(IllegalStateException::class.java) {
            PseudoShuffledIntProgression(intArrayOf(0, 1, 2, 3, 4, 5), intArrayOf(0, 1), intArrayOf(20, 21, 22, 23))
        }
    }

    @Test
    fun test6x2and1Tail() {
        val shuffle = PseudoShuffledIntProgression(intArrayOf(0, 1, 2, 3, 4, 5), intArrayOf(0, 1), intArrayOf(20))
        assertEquals(13, shuffle.size)
        val result = shuffle.toArray()
        assertArrayEquals(intArrayOf(0, 2, 4, 6, 8, 10, 20, 1, 3, 5, 7, 9, 11), result)
    }

    @Test
    fun testIsShuffledAndNoDuplicatesForFirst1000() {
        (2..1000).forEach {
            val shuffle = PseudoShuffledIntProgression(it, Random(9))
            val result = (0 until shuffle.size).map(shuffle::get)
            assertNotEquals(result.sorted(), result)
            assertEquals(result.size, result.toSet().size)
        }
    }

    @Test
    fun testIsRandom() {
        val randomSeed = 10
        val size = 1000
        val expected = IntArray(size) { it }.apply { shuffle(Random(randomSeed)) }
        val result = PseudoShuffledIntProgression(size, random = Random(randomSeed)) { it }.toArray()

        assertArrayEquals(expected, result)
    }

    @Test
    fun testForDifferentSides() {
        val size = 10000
        (1 until size).forEach { side ->
            val shuffle = PseudoShuffledIntProgression(size, Random) { side }
            assertEquals(size, shuffle.size)
            val values = shuffle.toArray().toSet()
            assertEquals(values.size, shuffle.size)
        }
    }

    @ParameterizedTest(name = "testCorrectInternalArraysAreCreatedFor{arguments}DifferentSides")
    @ValueSource(ints = [0, 1, 999, 1000])
    fun testCorrectInternalArraysAreCreatedForDifferentSides(size: Int) {
        (1 until size / 2).forEach { side ->
            val one = PseudoShuffledIntProgression(size, Random(98)) { side }
            val two = PseudoShuffledIntProgression(size, Random(98)) { it / side }
            assertEquals(one.size, two.size)
            assertArrayEquals(one.toArray(), two.toArray()) { "Fails for side = $side" }
        }
    }

    @Test
    fun testSpecification() {
        val shuffle = PseudoShuffledIntProgression(
            columns = intArrayOf(2, 1, 3, 0),
            rows = intArrayOf(2, 3, 4, 0, 1),
            tail = intArrayOf(22, 20, 21)
        )
        assertArrayEquals(
            intArrayOf(12, 7, 17, 2, 22, 13, 8, 18, 3, 20, 14, 9, 19, 4, 21, 10, 5, 15, 0, 11, 6, 16, 1),
            shuffle.toArray()
        )
    }

    @Test
    fun testIterator() {
        val shuffle = PseudoShuffledIntProgression(
            columns = intArrayOf(2, 1, 3, 0),
            rows = intArrayOf(2, 3, 4, 0, 1),
            tail = intArrayOf(22, 20, 21)
        )
        val expected = intArrayOf(12, 7, 17, 2, 22, 13, 8, 18, 3, 20, 14, 9, 19, 4, 21, 10, 5, 15, 0, 11, 6, 16, 1)
        shuffle.forEachIndexed { index, value ->
            assertEquals(expected[index], value)
        }
    }

    @Test
    fun testIteratorIteratesAllValuesExclusively() {
        val progression = PseudoShuffledIntProgression(
            columns = intArrayOf(0, 1, 2, 3),
            rows = intArrayOf(0, 1, 2),
            tail = intArrayOf(12)
        )
        val iterated = mutableListOf<Int>()
        val iterator = progression.iterator()
        while (iterator.hasNext()) {
            iterated.add(iterator.nextInt())
        }
        assertEquals(4 * 3 + 1, iterated.size)
        assertEquals((0 until 4 * 3 + 1).toList(), iterated.sorted())
    }

    @Test
    fun testNoIntOverflowWhenCalculateValue() {
        assertDoesNotThrow {
            PseudoShuffledIntProgression(Int.MAX_VALUE)[2147479015]
            PseudoShuffledIntProgression(Int.MAX_VALUE)[Int.MAX_VALUE - 1]
        }
    }

    @Test
    fun testNoDuplicates() {
        val size = 2000
        val set = PseudoShuffledIntProgression(size).toSet()
        assertEquals(size, set.size)
    }
}