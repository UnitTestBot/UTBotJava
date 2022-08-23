package org.utbot.fuzzer

import org.utbot.framework.plugin.api.UtModel
import kotlin.random.Random

/**
 * Mutates values and returns it.
 */
interface ModelMutator {

    /**
     * Mutates values set.
     *
     * Default implementation iterates through values and delegates to `mutate(FuzzedMethodDescription, Int, Random)`.
     */
    fun mutate(
        description: FuzzedMethodDescription,
        parameters: List<FuzzedValue>,
        random: Random,
    ) : List<FuzzedParameter> {
        return parameters
            .asSequence()
            .mapIndexedNotNull { index, fuzzedValue ->
                mutate(description, index, fuzzedValue, random)?.let { mutated ->
                    FuzzedParameter(index, mutated)
                }
            }
            .toList()
    }

    /**
     * Mutate a single value if it is possible.
     */
    fun mutate(
        description: FuzzedMethodDescription,
        index: Int,
        value: FuzzedValue,
        random: Random
    ) : FuzzedValue? {
        return null
    }

    fun UtModel.mutatedFrom(template: FuzzedValue, block: FuzzedValue.() -> Unit = {}): FuzzedValue {
        return FuzzedValue(this, template.createdBy).apply(block)
    }
}