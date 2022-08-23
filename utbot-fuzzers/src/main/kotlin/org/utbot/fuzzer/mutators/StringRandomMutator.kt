package org.utbot.fuzzer.mutators

import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelMutator
import org.utbot.fuzzer.flipCoin
import kotlin.random.Random

/**
 * Mutates string by adding and/or removal symbol at random position.
 */
object StringRandomMutator : ModelMutator {

    override fun mutate(
        description: FuzzedMethodDescription,
        index: Int,
        value: FuzzedValue,
        random: Random
    ): FuzzedValue? {
        return (value.model as? UtPrimitiveModel)
            ?.takeIf { it.classId == stringClassId }
            ?.let { model ->
                mutate(random, model.value as String).let {
                    UtPrimitiveModel(it).mutatedFrom(value) {
                        summary = "%var% = mutated string"
                    }
                }
            }
    }

    private fun mutate(random: Random, string: String): String {
        // we can miss some mutation for a purpose
        val position = random.nextInt(string.length + 1)
        var result: String = string
        if (random.flipCoin(probability = 50)) {
            result = tryRemoveChar(random, result, position) ?: string
        }
        if (random.flipCoin(probability = 50)) {
            result = tryAddChar(random, result, position)
        }
        return result
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