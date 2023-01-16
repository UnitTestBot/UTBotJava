package org.utbot.summary.fuzzer.names

import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.summary.MethodDescriptionSource

class MethodBasedNameSuggester(private val source: MethodDescriptionSource = MethodDescriptionSource.FUZZER) : NameSuggester {
    override fun suggest(
        description: FuzzedMethodDescription,
        values: List<FuzzedValue>,
        result: UtExecutionResult?
    ): Sequence<TestSuggestedInfo> {
        val compilableName = description.compilableName?.capitalize() ?: "Created"
        // See [Summarization.generateSummariesForTests].
        val suffix = if (source == MethodDescriptionSource.FUZZER) "ByFuzzer" else ""
        return sequenceOf(TestSuggestedInfo("test${compilableName}${suffix}"))
    }
}