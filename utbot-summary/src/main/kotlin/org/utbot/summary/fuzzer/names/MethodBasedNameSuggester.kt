package org.utbot.summary.fuzzer.names

import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue

class MethodBasedNameSuggester : NameSuggester {
    override fun suggest(description: FuzzedMethodDescription, values: List<FuzzedValue>, result: UtExecutionResult?): Sequence<TestSuggestedInfo> {
        return sequenceOf(TestSuggestedInfo("test${description.compilableName?.capitalize() ?: "Created"}ByFuzzer"))
    }
}