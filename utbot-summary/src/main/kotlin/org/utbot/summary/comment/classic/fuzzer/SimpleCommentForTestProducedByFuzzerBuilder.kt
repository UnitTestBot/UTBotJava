package org.utbot.summary.comment.classic.fuzzer

import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue

// TODO: https://github.com/UnitTestBot/UTBotJava/issues/1127
class SimpleCommentForTestProducedByFuzzerBuilder(
    description: FuzzedMethodDescription,
    values: List<FuzzedValue>,
    result: UtExecutionResult?
) {
    fun buildDocStatements(): List<DocStatement> {
        return listOf<DocStatement>(DocPreTagStatement(emptyList()))
    }
}