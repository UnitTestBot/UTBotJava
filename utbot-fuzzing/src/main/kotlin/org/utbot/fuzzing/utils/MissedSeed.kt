package org.utbot.fuzzing.utils

import org.utbot.fuzzing.Seed

class MissedSeed<T, R> : Iterable<T> {

    private val values = hashMapOf<T, Seed<T, R>>()

    operator fun set(value: T, seed: Seed<T, R>) {
        values[value] = seed
    }

    operator fun get(value: T): Seed<T, R>? {
        return values[value]
    }

    fun isEmpty(): Boolean {
        return values.size == 0
    }

    fun isNotEmpty(): Boolean = !isEmpty()

    override fun iterator(): Iterator<T> {
        return values.keys.iterator()
    }

}