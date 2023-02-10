package org.utbot.summary.comment.customtags.symbolic

import org.utbot.framework.plugin.api.ArtificialError
import org.utbot.framework.plugin.api.DocCustomTagStatement
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.exceptionOrNull
import org.utbot.summary.SummarySentenceConstants.CARRIAGE_RETURN
import org.utbot.summary.ast.JimpleToASTMap
import org.utbot.summary.comment.*
import org.utbot.summary.comment.classic.symbolic.*
import org.utbot.summary.comment.customtags.getClassReference
import org.utbot.summary.comment.customtags.getMethodReferenceForSymbolicTest
import org.utbot.summary.tag.TraceTagWithoutExecution
import soot.SootMethod

/**
 * Builds JavaDoc comments for generated tests using plugin's custom JavaDoc tags.
 */
class CustomJavaDocCommentBuilder(
    traceTag: TraceTagWithoutExecution,
    sootToAST: MutableMap<SootMethod, JimpleToASTMap>
) : SimpleCommentBuilder(traceTag, sootToAST, stringTemplates = StringsTemplatesPlural()) {

    /**
     * Collects statements for final JavaDoc comment.
     */
    fun buildDocStatements(method: SootMethod): List<DocStatement> {
        val comment = buildCustomJavaDocComment(method)
        val docStatementList =
            CustomJavaDocTagProvider().getPluginCustomTags().mapNotNull { it.generateDocStatement(comment) }
        return listOf(DocCustomTagStatement(docStatementList))
    }

    private fun buildCustomJavaDocComment(currentMethod: SootMethod): CustomJavaDocComment {
        val methodReference = getMethodReferenceForSymbolicTest(
            currentMethod.declaringClass.name,
            currentMethod.name,
            currentMethod.parameterTypes,
            false
        )
        val classReference = getClassReference(currentMethod.declaringClass.javaStyleName)

        val comment = CustomJavaDocComment(
            classUnderTest = classReference,
            methodUnderTest = methodReference,
        )

        val rootSentenceBlock = SimpleSentenceBlock(stringTemplates = stringTemplates)
        skippedIterations()
        buildSentenceBlock(traceTag.rootStatementTag, rootSentenceBlock, currentMethod)
        rootSentenceBlock.squashStmtText()

        // builds Throws exception section
        val thrownException = traceTag.result.exceptionOrNull()
        if (thrownException != null) {
            val exceptionName = thrownException.javaClass.name
            val reason = findExceptionReason(currentMethod, thrownException)
                .replace(CARRIAGE_RETURN, "")

            when (thrownException) {
                is ArtificialError -> comment.detectsSuspiciousBehavior = reason
                else -> comment.throwsException = "{@link $exceptionName} $reason"
            }
        }

        if (rootSentenceBlock.recursion != null) {
            comment.recursion += rootSentenceBlock.recursion?.first
            val insideRecursionSentence = rootSentenceBlock.recursion?.second?.toSentence()
            if (!insideRecursionSentence.isNullOrEmpty()) {
                comment.recursion += stringTemplates.insideRecursionSentence.format(insideRecursionSentence)
                    .replace(CARRIAGE_RETURN, "").trim()
            }
        }

        generateSequence(rootSentenceBlock) { it.nextBlock }.forEach {
            it.stmtTexts.forEach { statement ->
                processStatement(statement, comment)
            }

            it.invokeSentenceBlock?.let {
                comment.invokes += it.first.replace(CARRIAGE_RETURN, "")
                it.second.stmtTexts.forEach { statement ->
                    processStatement(statement, comment)
                }
            }

            it.iterationSentenceBlocks.forEach { (loopDesc, sentenceBlocks) ->
                comment.iterates += stringTemplates.iterationSentence.format(
                    stringTemplates.codeSentence.format(loopDesc),
                    numberOccurrencesToText(
                        sentenceBlocks.size
                    )
                ).replace(CARRIAGE_RETURN, "")
            }
        }

        return comment
    }

    private fun processStatement(
        statement: StmtDescription,
        comment: CustomJavaDocComment
    ) {
        when (statement.stmtType) {
            StmtType.Invoke -> comment.invokes += statement.description.replace(CARRIAGE_RETURN, "")
            StmtType.Condition -> comment.executesCondition += "{@code ${statement.description.replace(CARRIAGE_RETURN, "")}}"
            StmtType.Return -> comment.returnsFrom = "{@code ${statement.description.replace(CARRIAGE_RETURN, "")}}"
            StmtType.CaughtException -> comment.caughtException = "{@code ${statement.description.replace(CARRIAGE_RETURN, "")}}"
            StmtType.SwitchCase -> comment.switchCase = "{@code case ${statement.description.replace(CARRIAGE_RETURN, "")}}"
            StmtType.CountedReturn -> comment.countedReturn = "{@code ${statement.description.replace(CARRIAGE_RETURN, "")}}"
            StmtType.RecursionAssignment -> comment.recursion = "of {@code ${statement.description.replace(CARRIAGE_RETURN, "")}}"
        }
    }
}