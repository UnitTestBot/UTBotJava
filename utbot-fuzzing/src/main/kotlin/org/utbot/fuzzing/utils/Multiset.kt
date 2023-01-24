package org.utbot.fuzzing.utils

/**
 * Simple implementation of multiset.
 */
class Multiset<T> : Iterable<T> {

    private val values = hashMapOf<T, Long>()

    fun add(value: T): Long {
        val result = values.getOrDefault(value, 0L) + 1
        values[value] = result
        return result
    }

    operator fun get(value: T): Long {
        return values.getOrDefault(value, 0L)
    }

    fun isEmpty(): Boolean {
        return values.size == 0
    }

    fun isNotEmpty(): Boolean = !isEmpty()

    fun removeAll(value: T) {
        values.remove(value)
    }

    override fun iterator(): Iterator<T> {
        return values.keys.iterator()
    }

}