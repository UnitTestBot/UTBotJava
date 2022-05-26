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
            // todo create lazy random algo for this because this method can cause OOME even we take only one value
            val permutation = IntArray(combinations.size) { it }
            permutation.shuffle(random)
            permutation.asSequence().map(combinations::get)
        } else {
            combinations.asSequence()
        }
        return sequence.map { combination ->
            combination.mapIndexedTo(mutableListOf()) { element, value -> lists[element][value] }
        }.iterator()
    }
}