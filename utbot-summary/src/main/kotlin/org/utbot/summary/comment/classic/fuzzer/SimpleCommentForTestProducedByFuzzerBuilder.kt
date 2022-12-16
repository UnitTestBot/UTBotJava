package org.utbot.summary.comment.classic.fuzzer

import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.summary.SummarySentenceConstants.NEW_LINE
import org.utbot.summary.comment.customtags.getClassReference
import org.utbot.summary.comment.customtags.getFullClassName
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
        val canonicalName = methodDescription.canonicalName
        val isNested = methodDescription.isNested

        val result = if (packageName != null && className != null && methodName != null) {
            val fullClassName = getFullClassName(canonicalName, packageName, className, isNested)

            val methodReference = getMethodReferenceForFuzzingTest(
                fullClassName,
                methodName,
                methodDescription.parameters,
                false
            )

            val classReference = getClassReference(fullClassName)

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