package org.utbot.fuzzer

import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Generates pseudo random values from 0 to size exclusive.
 *
 * In general there are 2 ways to get random order for a given range:
 * 1. Create an array of target size and shuffle values.
 * 2. Create a set of generated values and generate new values until they're unique.
 *
 * Both cases cause high memory usage when target number of values is big.
 *
 * The memory usage can be reduced by using a pseudo-random sequence.
 *
 * Algorithm to create pseudo-random sequence:
 * 1. Separate a sequence into 2 parts: matrix-part and tail
 * 2. Transform linear representation of the matrix into 2-array view with cols × rows arrays
 * 3. The tail array stores values that are greater than the biggest value of the matrix (cols × rows)
 * 4. Shuffle all arrays
 * 5. For a given index the target value is taken from the matrix or from the tail array.
 *
 * Example, input size = 23
 * ```
 * matrix             tail
 * -----------------------
 *  0  5 10 15        20
 *  1  6 11 16        21
 *  2  7 12 17        22
 *  3  8 13 18
 *  4  9 14 19
 * ```
 * Columns are shuffled:
 *
 * ```
 * matrix             tail
 * -----------------------
 * 10  5 15  0        20
 * 11  6 16  1        21
 * 12  7 17  2        22
 * 13  8 18  3
 * 14  9 19  4
 * ```
 * Rows and tail are shuffled
 * ```
 * matrix             tail
 * -----------------------
 * 12  7 17  2        22
 * 13  8 18  3        20
 * 14  9 19  4        21
 * 10  5 15  0
 * 11  6 16  1
 * ```
 * Merge matrix and tail:
 * ```
 * 12  7 17  2 22
 * 13  8 18  3 20
 * 14  9 19  4 21
 * 10  5 15  0 ×
 * 11  6 16  1 ×
 * ```
 *
 * Write down the sequence: `[12, 7, 17, 2, 22, 13, 8, 18, 3, 20, 14, 9, 19, 4, 21, 10, 5, 15, 0, 11, 6, 16, 1]`.
 *
 * This algorithm requires only `537 552 bytes (~ 550 KB)` instead of `Int.MAX_VALUE * 4 = 8 589 934 588 bytes (~ 8 GB)`
 * in the simple array-shuffle algorithm.
 *
 */
class PseudoShuffledIntProgression : Iterable<Int> {
    private val top: IntArray
    private val left: IntArray
    private val tail: IntArray

    val size: Int

    constructor(size: Int, random: Random = Random) : this (size, random, { sqrt(it.toDouble()).toInt() })

    /**
     * Test only constructor
     */
    internal constructor(size: Int, random: Random, side: (Int) -> Int) {
        check(size >= 0) { "Size must be positive or 0 but current value is $size" }
        this.size = size
        var topSize = side(size)
        if (topSize > 0 && topSize > size / topSize) {
            topSize = size / topSize
        }
        check(topSize > 0 || size == 0) { "Side of matrix must be greater than 0 but $topSize <= 0" }

        top = IntArray(size = topSize) { it }.apply { shuffle(random) }
        left = IntArray(size = if (topSize == 0) 0 else size / topSize) { it }.apply { shuffle(random) }
        check(top.size <= left.size) { "Error in internal array state" }

        val rectangle = top.size * left.size
        tail = IntArray(size - rectangle) { it + rectangle }.apply { shuffle(random) }
    }

    /**
     * Test only constructor
     */
    internal constructor(top: IntArray, left: IntArray, tail: IntArray) {
        check(left.size >= tail.size) { "Tail cannot be placed into 1 column of the target matrix" }
        this.top = top
        this.left = left
        this.tail = tail
        this.size = top.size * left.size + tail.size
    }

    /**
     * Returns a unique pseudo-random index for the current one.
     */
    operator fun get(index: Int): Int {
        check(index in 0 until size) { "Index out of bounds: $index >= $size" }
        val t = top.size
        val l = left.size
        val e = t + 1
        var i = index % e
        var j = index / e
        return if (i == t && j < tail.size) {
            tail[j]
        } else {
            val o = ((index - tail.size * e) / t).toLong()
            if (o > 0) {
                i = ((index + o) % e).toInt()
                j = ((index + o) / e).toInt()
            }
            top[i] * l + left[j]
        }
    }

    fun toArray(): IntArray = IntArray(size, this::get)

    override fun iterator(): IntIterator = object : IntIterator() {
        var current = 0
        override fun hasNext() = current < size
        override fun nextInt() = get(current++)
    }
}