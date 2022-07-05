package org.utbot.fuzzer

import kotlin.random.Random

/**
 * Creates iterable for all values of cartesian product of `lists`.
 */
class CartesianProduct<T>(
    private val lists: List<List<T>>,
    private val random: Random? = null
): Iterable<List<T>> {

    fun asSequence(): Sequence<List<T>> = iterator().asSequence()

    override fun iterator(): Iterator<List<T>> {
        val combinations = Combinations(*lists.map { it.size }.toIntArray())
        val sequence = if (random != null) {
            val permutation = PseudoShuffledIntProgression(combinations.size, random)
            (0 until combinations.size).asSequence().map { combinations[permutation[it]] }
        } else {
            combinations.asSequence()
        }
        return sequence.map { combination ->
            combination.mapIndexedTo(mutableListOf()) { element, value -> lists[element][value] }
        }.iterator()
    }
}