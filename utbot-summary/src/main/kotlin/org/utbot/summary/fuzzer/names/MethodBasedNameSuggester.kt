package org.utbot.summary.fuzzer.names

import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.summary.MethodDescriptionSource
import java.util.*

class MethodBasedNameSuggester(private val source: MethodDescriptionSource = MethodDescriptionSource.FUZZER) : NameSuggester {
    override fun suggest(
        description: FuzzedMethodDescription,
        values: List<FuzzedValue>,
        result: UtExecutionResult?
    ): Sequence<TestSuggestedInfo> {
        val compilableName = description.compilableName?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            ?: "Created"
        // See [Summarization.generateSummariesForTests].
        val suffix = if (source == MethodDescriptionSource.FUZZER) "ByFuzzer" else ""
        return sequenceOf(TestSuggestedInfo("test${compilableName}${suffix}"))
    }
}