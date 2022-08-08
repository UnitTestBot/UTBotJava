package org.utbot.summary.fuzzer.names

import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue

/**
 * Name suggester generates a sequence of suggested test information such as:
 * - method test name.
 * - display name.
 */
interface NameSuggester {
    fun suggest(description: FuzzedMethodDescription, values: List<FuzzedValue>, result: UtExecutionResult?): Sequence<TestSuggestedInfo>
}