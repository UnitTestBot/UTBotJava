package org.utbot.fuzzing.seeds

import org.utbot.fuzzing.Mutation
import org.utbot.fuzzing.StringMutations

open class StringValue(
    val valueProvider: () -> String,
    override val lastMutation: Mutation<out StringValue>? = null,
    override val mutatedFrom: StringValue? = null,
) : KnownValue<StringValue> {

    constructor(
        value: String,
        lastMutation: Mutation<out StringValue>? = null,
        mutatedFrom: StringValue? = null
    ) : this(valueProvider = { value }, lastMutation, mutatedFrom)

    val value by lazy { valueProvider() }

    override fun mutations(): List<Mutation<out StringValue>> {
        return listOf(
            StringMutations.AddCharacter,
            StringMutations.RemoveCharacter,
            StringMutations.ShuffleCharacters,
        )
    }

    override fun toString(): String {
        return "\"" + value + "\""
    }
}