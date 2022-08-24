package org.utbot.fuzzer

import kotlin.math.pow
import kotlin.random.Random

/**
 * Stores information that can be useful for fuzzing such as coverage, run count, etc.
 */
interface FuzzerStatistics<K> {

    val seeds: Collection<K>

    /**
     * Returns a random seed to process.
     */
    fun randomSeed(random: Random): K?

    fun randomValues(random: Random): List<FuzzedValue>?

    fun executions(seed: K): Int

    operator fun get(seed: K): List<FuzzedValue>?

    fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean {
        return !isEmpty()
    }
}

class TrieBasedFuzzerStatistics<V>(
    private val values: LinkedHashMap<Trie.Node<V>, List<FuzzedValue>> = linkedMapOf()
) : FuzzerStatistics<Trie.Node<V>> {

    override val seeds: Collection<Trie.Node<V>>
        get() = values.keys

    override fun randomSeed(random: Random): Trie.Node<V>? {
        return values.keys.elementAtOrNull(randomIndex(random))
    }

    override fun isEmpty(): Boolean {
        return values.isEmpty()
    }

    override fun isNotEmpty(): Boolean {
        return values.isNotEmpty()
    }

    override fun randomValues(random: Random): List<FuzzedValue>? {
        return values.values.elementAtOrNull(randomIndex(random))
    }

    private fun randomIndex(random: Random): Int {
        val frequencies = DoubleArray(values.size).also { f ->
            values.keys.forEachIndexed { index, key ->
                f[index] = 1 / key.count.toDouble().pow(2)
            }
        }
        return random.chooseOne(frequencies)
    }

    override fun get(seed: Trie.Node<V>): List<FuzzedValue>? {
        return values[seed]
    }

    override fun executions(seed: Trie.Node<V>): Int {
        return seed.count
    }

}