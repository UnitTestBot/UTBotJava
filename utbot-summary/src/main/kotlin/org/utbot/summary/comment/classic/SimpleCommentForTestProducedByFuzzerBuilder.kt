package org.utbot.summary.comment.classic

import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue

class SimpleCommentForTestProducedByFuzzerBuilder(
    description: FuzzedMethodDescription,
    values: List<FuzzedValue>,
    result: UtExecutionResult?
) {
    fun buildDocStatements(): List<DocStatement> {
        //val sentenceBlock = buildSentenceBlock(currentMethod)
        //val docStmts = toDocStmts(sentenceBlock)

        /* if (docStmts.isEmpty()) {
             return emptyList()
         }*/

        return listOf<DocStatement>(DocPreTagStatement(emptyList()))
    }
}
