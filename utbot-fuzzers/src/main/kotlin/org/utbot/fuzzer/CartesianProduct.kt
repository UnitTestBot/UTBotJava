package org.utbot.fuzzer

/**
 * Creates iterable for all values of cartesian product of `lists`.
 */
class CartesianProduct<T>(private val lists: List<List<T>>): Iterable<List<T>> {

    fun asSequence(): Sequence<List<T>> = iterator().asSequence()

    override fun iterator(): Iterator<List<T>> {
        return Combinations(*lists.map { it.size }.toIntArray())
            .asSequence()
            .map { combination ->
                combination.mapIndexedTo(mutableListOf()) { element, value -> lists[element][value] }
            }
            .iterator()
    }
}