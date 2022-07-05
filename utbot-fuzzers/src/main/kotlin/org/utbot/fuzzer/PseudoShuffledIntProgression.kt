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
 * Algorithm to create pseudo-random sequence of length L:
 * 1. Take first K elements to create a matrix of size COLS × ROWS in such way that K = COLS * ROWS and ROWS >= L - K.
 * 2. Move last L - K elements into a new array tail.
 * 3. Any index N from [0, K) can be calculated by using 2 numbers: number of column (i) and number of row (j) as follows:
 *      `N = i * ROWS + j` where `i = N % COLS` and `j = N / COLS`.
 *      In such case the index N increases from top to bottom and left to right.
 * 4. Tail contains all values from [K, L).
 * 5. Shuffle matrix columns, matrix rows and the tail.
 * 6. Add the tail as a column to the matrix. Since ROWS >= L - K there are missing values for i = COLS and j >= L - K.
 * 7. Write down all values except missing from left to right and top to bottom.
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
 * Instead of storing matrix itself only column and row numbers can be stored. Therefore, the 5th step of the algorithm
 * can be changed into this:
 *
 * 5. Shuffle column numbers, row numbers and tail.
 *
 * In this case any value from the matrix can be calculated as follows:
 *      `N = column[i] * ROWS + rows[j]`, where column and rows are shuffled column and row numbers arrays.
 *
 * Using number arrays instead of the matrix this algorithm requires only
 * `537 552 bytes (~ 550 KB)` compared to `Int.MAX_VALUE * 4 = 8 589 934 588 bytes (~ 8 GB)`
 * when using simple array-shuffle algorithm.
 */
class PseudoShuffledIntProgression : Iterable<Int> {
    private val columnNumber: IntArray
    private val rowNumber: IntArray
    private val tail: IntArray

    val size: Int

    constructor(size: Int, random: Random = Random) : this(size, random, { sqrt(it.toDouble()).toInt() })

    /**
     * Test only constructor
     */
    internal constructor(size: Int, random: Random, columns: (Int) -> Int) {
        check(size >= 0) { "Size must be positive or 0 but current value is $size" }
        this.size = size
        var cols = columns(size)
        if (cols > 0 && cols > size / cols) {
            cols = size / cols
        }
        check(cols > 0 || size == 0) { "Side of matrix must be greater than 0 but $cols <= 0" }

        columnNumber = IntArray(size = cols) { it }.apply { shuffle(random) }
        rowNumber = IntArray(size = if (cols == 0) 0 else size / cols) { it }.apply { shuffle(random) }
        check(columnNumber.size <= rowNumber.size) { "Error in internal array state: number of rows shouldn't be less than number of columns" }

        val rectangle = columnNumber.size * rowNumber.size
        tail = IntArray(size - rectangle) { it + rectangle }.apply { shuffle(random) }
    }

    /**
     * Test only constructor
     */
    internal constructor(columns: IntArray, rows: IntArray, tail: IntArray) {
        check(rows.size >= tail.size) { "Tail cannot be placed into 1 column of the target matrix" }
        this.columnNumber = columns
        this.rowNumber = rows
        this.tail = tail
        this.size = columns.size * rows.size + tail.size
    }

    /**
     * Returns a unique pseudo-random index for the current one.
     *
     * To calculate correct value of the merged matrix as described before
     * let's look at not shuffled merged matrix with size 2 × 6 and the tail with 2 elements.
     *
     * ```
     * 0  6 13
     * 1  7 14
     * 2  9  ×
     * 3 10  ×
     * 4 11  ×
     * 5 12  ×
     * ```
     *
     * The index moves from left to right and top to bottom, so the result sequence should be
     *      `[0, 6, 13, 1, 7, 14, 2, 9, 3, 10, 4, 11, 5, 12]`
     * but the correct index in this matrix cannot be calculated just as `i * ROWS + j` because of missing values.
     * To correct the index a property shift should be used. Let's transform the matrix into a matrix of shift
     * that should be added to the index for jumping over missing values:
     *
     * ```
     * 0  0  0
     * 0  0  0
     * 0  0  1
     * 1  2  2
     * 3  3  ×
     * ×  ×  ×
     * ```
     *
     * The matrix can be divided into 3 parts that calculates correct indices:
     *
     * ```
     * 0  0 | 0
     * 0  0 | 0
     * _____|___
     * 0  0   1
     * 1  2   2
     * 3  3   ×
     * ×  ×   ×
     * ```
     *
     * 1. Top left doesn't change the index and values are taken from the matrix.
     * 2. Top right doesn't change the index and values are taken from the tail.
     * 3. Bottom uses shifts to change index and values are taken from the matrix.
     *
     * To clarify general rule for calculating a shift let's look at merged matrix
     * where every row has COLS values and TAILS missing values like this:
     * ```
     * 1:   N1 N2 N3 .. N_COLS | M1 M2 .. M_TAILS
     * 2:   N1 N2 N3 .. N_COLS | M1 M2 .. M_TAILS
     * ...
     * ROW: N1 N2 N3 .. N_COLS | M1 M2 .. M_TAILS
     * ```
     * It can be shown that the shift changes every COLS values on TAILS, therefore the shift can be calculated as follows:
     * ```
     * shift = (index / COLS) * TAILS
     * ```
     *
     * **Examples**
     *
     * COLS = 3, TAILS = 1:
     * ```
     *  0  0  0  1
     *  1  1  2  2
     *  2  3  3  3
     * ...
     * ```
     *
     * COLS = 1, TAILS = 3
     * ```
     *  0  3  6  9
     * 12 15 18 21
     * 24 27 30 33
     * ...
     * ```
     *
     * COLS = 2, TAILS = 2
     * ```
     *  0  0  2  2
     *  4  4  6  6
     *  8  8 10 10
     *  ...
     * ```
     */
    operator fun get(index: Int): Int {
        check(index in 0 until size) { "Index out of bounds: $index >= $size" }
        val cols = columnNumber.size
        val rows = rowNumber.size
        val e = cols + 1
        var i = index % e
        var j = index / e
        return if (i == cols && j < tail.size) {
            // top right case
            tail[j]
        } else {
            // first tail.size * e values can be calculated without index shift
            // o < 0 is the top left case
            // o >= 0 is the bottom case with COLS = cols and TAILS = 1
            val o = ((index - tail.size * e) / cols).toLong()
            if (o > 0) {
                i = ((index + o) % e).toInt()
                j = ((index + o) / e).toInt()
            }
            columnNumber[i] * rows + rowNumber[j]
        }
    }

    fun toArray(): IntArray = IntArray(size, this::get)

    override fun iterator(): IntIterator = object : IntIterator() {
        var current = 0
        override fun hasNext() = current < size
        override fun nextInt() = get(current++)
    }
}