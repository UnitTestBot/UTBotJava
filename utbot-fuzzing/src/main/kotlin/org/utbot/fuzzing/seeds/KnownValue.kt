package org.utbot.fuzzing.seeds

import org.utbot.fuzzing.Mutation

interface KnownValue<T : KnownValue<T>> {
    val lastMutation: Mutation<out T>?
        get() = null

    val mutatedFrom: T?
        get() = null

    fun mutations(): List<Mutation<out T>> {
        return emptyList()
    }
}