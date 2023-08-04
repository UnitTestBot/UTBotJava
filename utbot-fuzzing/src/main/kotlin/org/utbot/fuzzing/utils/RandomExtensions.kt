package org.utbot.fuzzing.utils

import kotlin.random.Random

/**
 * Chooses a random value using frequencies.
 *
 * If value has greater frequency value then it would be chosen with greater probability.
 *
 * @return the index of the chosen item.
 */
fun Random.chooseOne(frequencies: DoubleArray): Int {
    val total = frequencies.sum()
    val value = nextDouble(total)
    var nextBound = 0.0
    frequencies.forEachIndexed { index, bound ->
        check(bound >= 0) { "Frequency must not be negative" }
        nextBound += bound
        if (value < nextBound) return index
    }
    error("Cannot find next index")
}

/**
 * Tries a value.
 *
 * If a random value is less than [probability] returns true.
 */
fun Random.flipCoin(probability: Int): Boolean {
    if (probability == 0) return false
    if (probability == 100) return true
    check(probability in 0 .. 100) { "probability must in range [0, 100] but $probability is provided" }
    return nextInt(1, 101) <= probability
}

fun Long.invertBit(bitIndex: Int): Long {
    return this xor (1L shl bitIndex)
}

fun Int.hex(): String =
    toString(16)
