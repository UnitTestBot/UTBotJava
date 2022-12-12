package org.utbot.fuzzing.seeds

import org.utbot.fuzzing.Mutation

interface KnownValue {
    val lastMutation: Mutation<KnownValue>?
        get() = null

    val mutatedFrom: KnownValue?
        get() = null

    fun mutations(): List<Mutation<KnownValue>>
}