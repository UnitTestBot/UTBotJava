package org.utbot.summary.comment

import org.utbot.framework.plugin.api.DocCustomTagStatement
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.exceptionOrNull
import org.utbot.summary.SummarySentenceConstants
import org.utbot.summary.SummarySentenceConstants.CARRIAGE_RETURN
import org.utbot.summary.ast.JimpleToASTMap
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
        val methodReference = getMethodReference(
            currentMethod.declaringClass.name,
            currentMethod.name,
            currentMethod.parameterTypes
        )
        val classReference = getClassReference(currentMethod.declaringClass.javaStyleName)

        val comment = CustomJavaDocComment(
            classUnderTest = classReference.replace(CARRIAGE_RETURN, ""),
            methodUnderTest = methodReference.replace(CARRIAGE_RETURN, ""),
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

            comment.throwsException = "{@link $exceptionName} $reason".replace(CARRIAGE_RETURN, "")
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