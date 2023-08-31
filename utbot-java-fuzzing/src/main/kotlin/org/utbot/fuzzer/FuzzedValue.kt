package org.utbot.fuzzer

import org.utbot.framework.plugin.api.UtModel

/**
 * Fuzzed Value stores information about concrete UtModel, reference to [ModelProvider]
 * and reasons about why this value was generated.
 *
 * [summary] is a piece of useful information that clarify why this value has a concrete value.
 *
 * It supports a special character `%var%` that is used as a placeholder for parameter name.
 *
 * For example:
 * 1. `%var% = 2` for a value that have value 2
 * 2. `%var% >= 4` for a value that shouldn't be less than 4
 * 3. `foo(%var%) returns true` for values that should be passed as a function parameter
 * 4. `%var% has special characters` to describe content
 */
open class FuzzedValue(
    val model: UtModel,
    var summary: String? = null,
) {
    override fun toString(): String {
        return "FuzzedValue: $summary"
    }
}