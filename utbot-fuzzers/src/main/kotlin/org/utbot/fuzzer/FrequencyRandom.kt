package org.utbot.fuzzer

import kotlin.random.Random

/**
 * Frequency random returns [nextIndex] with given probabilities.
 */
class FrequencyRandom(var random: Random = Random) {
    private var total = 1.0
    private var bound: DoubleArray = DoubleArray(1) { 1.0 }
    val size: Int
        get() = bound.size

    /**
     * Updates internals to generate values using [frequencies] and custom function [forEach].
     *
     * For example, it is possible implement logic where the source list of [frequencies] is inverted.
     *
     * ```
     * prepare(listOf(20., 80.), forEach = { 100 - it })
     *
     * In this case second value (that has 80.)in the [frequencies] list will be found in 20% of all cases.
     */
    fun <T : Number> prepare(frequencies: List<T>, forEach: (T) -> Double = { it.toDouble() }) {
        bound = DoubleArray(frequencies.size) { forEach(frequencies[it]) }
        for (i in 1 until bound.size) {
            bound[i] = bound[i] + bound[i - 1]
        }
        total = if (bound.isEmpty()) 0.0 else bound.last()
    }

    fun nextIndex(): Int {
        val value = random.nextDouble(total)
        for (index in bound.indices) {
            if (value < bound[index]) {
                return index
            }
        }
        error("Cannot find next index")
    }
}