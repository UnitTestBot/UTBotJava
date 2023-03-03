package org.utbot.python.fuzzing.value

import org.utbot.fuzzing.Mutation
import org.utbot.fuzzing.seeds.KnownValue

class ObjectValue : KnownValue {
    override fun mutations(): List<Mutation<KnownValue>> {
        return emptyList()
    }
}
