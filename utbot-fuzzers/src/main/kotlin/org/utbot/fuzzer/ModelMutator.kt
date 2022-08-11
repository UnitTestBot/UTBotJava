package org.utbot.fuzzer

import org.utbot.framework.plugin.api.UtModel
import kotlin.random.Random

/**
 * Mutates values and returns it.
 *
 * Mutator can be not applied using [probability] as pivot.
 * In this case unchanged values is returned.
 */
interface ModelMutator {

    /**
     * The probability of applying this mutator. Can be ignored in some implementations.
     */
    val probability: Int

    /**
     * Mutates values set.
     *
     * Default implementation iterates through values and delegates to `mutate(FuzzedMethodDescription, Int, Random)`.
     */
    fun mutate(
        description: FuzzedMethodDescription,
        parameters: List<FuzzedValue>,
        random: Random,
    ) : List<FuzzedValue> {
        return parameters.mapIndexed { index, fuzzedValue ->
            mutate(description, index, fuzzedValue, random) ?: fuzzedValue
        }
    }

    /**
     * Mutate a single value if it is possible.
     *
     * Default implementation mutates the value with given [probability] or returns `null`.
     */
    fun mutate(
        description: FuzzedMethodDescription,
        index: Int,
        value: FuzzedValue,
        random: Random
    ) : FuzzedValue? {
        return if (random.nextInt(1, 101) < probability) value else null
    }

    fun UtModel.mutatedFrom(template: FuzzedValue, block: FuzzedValue.() -> Unit = {}): FuzzedValue {
        return FuzzedValue(this, template.createdBy, template.mutatedBy + this@ModelMutator).apply(block)
    }
}