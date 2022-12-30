package org.utbot.fuzzing.seeds

import org.utbot.fuzzing.Mutation
import org.utbot.fuzzing.utils.flipCoin
import kotlin.random.Random

class StringValue(val value: String) : KnownValue {

    override fun mutations(): List<Mutation<KnownValue>> {
        return listOf(Mutation { source, random, configuration ->
            require(source == this)
            // we can miss some mutation for a purpose
            val position = random.nextInt(value.length + 1)
            var result: String = value
            if (random.flipCoin(configuration.probStringRemoveCharacter)) {
                result = tryRemoveChar(random, result, position) ?: value
            }
            if (result.length < configuration.maxStringLengthWhenMutated && random.flipCoin(configuration.probStringAddCharacter)) {
                result = tryAddChar(random, result, position)
            }
            StringValue(result)
        })
    }

    private fun tryAddChar(random: Random, value: String, position: Int): String {
        val charToMutate = if (value.isNotEmpty()) {
            value.random(random)
        } else {
            // use any meaningful character from the ascii table
            random.nextInt(33, 127).toChar()
        }
        return buildString {
            append(value.substring(0, position))
            // try to change char to some that is close enough to origin char
            val charTableSpread = 64
            if (random.nextBoolean()) {
                append(charToMutate - random.nextInt(1, charTableSpread))
            } else {
                append(charToMutate + random.nextInt(1, charTableSpread))
            }
            append(value.substring(position, value.length))
        }
    }

    private fun tryRemoveChar(random: Random, value: String, position: Int): String? {
        if (position >= value.length) return null
        val toRemove = random.nextInt(value.length)
        return buildString {
            append(value.substring(0, toRemove))
            append(value.substring(toRemove + 1, value.length))
        }
    }
}