package org.utbot.fuzzer

/**
 * Enumerates all possible combinations for a given list of maximum numbers of elements for every position.
 *
 * For any index between 0 and [size] excluded it returns the unique combination as an array with
 * values that are between 0 and corresponding maximum number from the `elementNumbers`.
 *
 * For example for a given list {2, 3} the following combinations get be found by corresponding index:
 *
 * ```
 * 0 - {0, 0}
 * 1 - {0, 1}
 * 2 - {0, 2}
 * 3 - {1, 0}
 * 4 - {1, 1}
 * 5 - {1, 2}
 * ```
 *
 * Use this class to iterate through all combinations of some data, like this:
 *
 * ```
 * val dataList = arrayListOf(
 *   listOf("One", "Two"),
 *   listOf(1.0, 2.0, Double.NaN),
 * )
 * Combinations(*dataList.map { it.size }.toIntArray()).forEach { combination ->
 *   println("${dataList[0][combination[0]]} ${dataList[1][combination[1]]}")
 * }
 * ```
 *
 * This example iterates through all values that are result of cartesian product of input lists:
 *
 * ```
 * One 1.0
 * One 2.0
 * One NaN
 * Two 1.0
 * Two 2.0
 * Two NaN
 * ```
 *
 * Simpler way to iterate through all combinations by using [CartesianProduct]:
 *
 * ```
 * CartesianProduct(listOf(
 *   listOf("One", "Two"),
 *   listOf(1.0, 2.0, Double.NaN)
 * )).forEach {
 *   println("${it[0]} ${it[1]}")
 * }
 * ```
 */
class Combinations(vararg elementNumbers: Int): Iterable<IntArray> {
    /**
     * Internal field that keeps a count of combinations for particular position.
     *
     * The count is calculated from left to right for, for example with a given elementNumbers [4, 6, 2]
     * the result is: `[4 * 6 * 2, 6 * 2, 2] = [48, 12, 2]`. Therefore, the total count can be obtained for a subarray by index:
     * - `[..., ..., 2] = count[2] = 2`
     * - `[..., 12, 2] = count[1] = 12`
     * - `[48, 12, 2] = count[0] = 48`
     *
     * The total count of all possible combinations is therefore `count[0]`.
     */
    private val count: LongArray
    val size: Long
        get() = if (count.isEmpty()) 0 else count[0]

    init {
        val badValue = elementNumbers.indexOfFirst { it <= 0 }
        if (badValue >= 0) {
            throw IllegalArgumentException("Max value must be at least 1 to build combinations, but ${elementNumbers[badValue]} is found at position $badValue (list: $elementNumbers)")
        }
        count = LongArray(elementNumbers.size) { elementNumbers[it].toLong() }
        for (i in count.size - 2 downTo 0) {
            try {
                count[i] = StrictMath.multiplyExact(count[i], count[i + 1])
            } catch (e: ArithmeticException) {
                throw TooManyCombinationsException("Long overflow: ${count[i]} * ${count[i + 1]}")
            }
        }
    }

    override fun iterator(): Iterator<IntArray> {
        return (0 until size).asSequence().map { get(it) }.iterator()
    }

    /**
     * Returns combination by its index.
     *
     * Algorithm is similar to base conversion. Thus [Combinations] can be used to generate all numbers with given
     * number of digits. This example prints all numbers from 000 to 999:
     *
     * ```
     * Combinations(10, 10, 10).forEach { digits ->
     *   println(digits.joinToString(separator = "") { it.toString() })
     * }
     * ```
     */
    operator fun get(value: Long, target: IntArray = IntArray(count.size)): IntArray {
        if (value >= size) {
            throw java.lang.IllegalArgumentException("Only $size values allowed")
        }
        if (target.size != count.size) {
            throw java.lang.IllegalArgumentException("Different array sizes: ${target.size} != ${count.size} ")
        }
        var rem = value
        for (i in target.indices) {
            target[i] = if (i < target.size - 1) {
                val res = checkBoundsAndCast(rem / count[i + 1])
                rem %= count[i + 1]
                res
            } else {
                checkBoundsAndCast(rem)
            }
        }
        return target
    }

    private fun checkBoundsAndCast(value: Long): Int {
        check(value >= 0 && value < Int.MAX_VALUE) { "Value is out of bounds: $value" }
        return value.toInt()
    }
}

class TooManyCombinationsException(msg: String) : RuntimeException(msg)