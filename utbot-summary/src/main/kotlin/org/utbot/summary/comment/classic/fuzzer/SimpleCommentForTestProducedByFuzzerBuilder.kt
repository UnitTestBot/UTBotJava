package org.utbot.summary.comment.classic.fuzzer

import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.summary.SummarySentenceConstants
import org.utbot.summary.SummarySentenceConstants.NEW_LINE
import org.utbot.summary.comment.customtags.fuzzer.CommentWithCustomTagForTestProducedByFuzzer
import org.utbot.summary.comment.customtags.getClassReference
import org.utbot.summary.comment.customtags.getMethodReferenceForFuzzingTest

class SimpleCommentForTestProducedByFuzzerBuilder(
    val methodDescription: FuzzedMethodDescription,
    val values: List<FuzzedValue>,
    val result: UtExecutionResult?
) {
    fun buildDocStatements(): List<DocStatement> {
        val packageName = methodDescription.packageName
        val className = methodDescription.className
        val methodName = methodDescription.compilableName

        val result = if (packageName != null && className != null && methodName != null) {
            val fullClassName = "$packageName.$className"

            val methodReference = getMethodReferenceForFuzzingTest(
                fullClassName,
                methodName,
                methodDescription.parameters,
                false
            ).replace(SummarySentenceConstants.CARRIAGE_RETURN, "")

            val classReference = getClassReference(fullClassName).replace(SummarySentenceConstants.CARRIAGE_RETURN, "")


            val docStatements = mutableListOf<DocStatement>()
            docStatements.add(DocRegularStmt("Class under test: $classReference$NEW_LINE"))
            docStatements.add(DocRegularStmt("Method under test: $methodReference$NEW_LINE"))
            docStatements
        } else {
            emptyList()
        }

        return listOf<DocStatement>(DocPreTagStatement(result))
    }
}