package org.utbot.fuzzer.mutators

import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelMutator
import kotlin.random.Random

/**
 * Mutates string by adding and/or removal symbol at random position.
 *
 * Adding or removal can be applied with a given [probability].
 */
class StringRandomMutator(override val probability: Int = 50) : ModelMutator {

    init {
        check(probability in 0 .. 100) { "Probability must be in range 0..100" }
    }

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
        if (random.nextInt(1, 101) <= probability) {
            result = tryRemoveChar(random, result, position) ?: string
        }
        if (random.nextInt(1, 101) <= probability) {
            result = tryAddChar(random, result, position)
        }
        return result
    }

    private fun tryAddChar(random: Random, value: String, position: Int): String {
        val charToMutate = if (value.isNotEmpty()) {
            value[random.nextInt(value.length)]
        } else {
            random.nextInt(1, 65536).toChar()
        }
        return buildString {
            append(value.substring(0, position))
            if (random.nextBoolean()) {
                append(charToMutate - random.nextInt(1, 128))
            } else {
                append(charToMutate + random.nextInt(1, 128))
            }
            append(value.substring(position, value.length))
        }
    }

    private fun tryRemoveChar(random: Random, value: String, position: Int): String? {
        if (value.isEmpty() || position >= value.length) return null
        val toRemove = random.nextInt(value.length)
        return buildString {
            append(value.substring(0, toRemove))
            append(value.substring(toRemove + 1, value.length))
        }
    }
}