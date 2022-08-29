package org.utbot.summary.comment

import org.utbot.framework.plugin.api.DocCustomTagStatement
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.exceptionOrNull
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

        val customJavaDocComment = CustomJavaDocComment(
            classUnderTest = classReference,
            methodUnderTest = methodReference,
        )

        val rootSentenceBlock = SimpleSentenceBlock(stringTemplates = stringTemplates)
        skippedIterations()
        buildSentenceBlock(traceTag.rootStatementTag, rootSentenceBlock, currentMethod)
        rootSentenceBlock.squashStmtText()

        // builds Throws exception section
        val thrownException = traceTag.result.exceptionOrNull()
        val exceptionThrow: String? = if (thrownException == null) {
            traceTag.result.exceptionOrNull()?.let { it::class.qualifiedName }
        } else {
            val exceptionName = thrownException.javaClass.name
            val reason = findExceptionReason(currentMethod, thrownException)
            "{@link $exceptionName} $reason"
        }

        if (exceptionThrow != null) {
            customJavaDocComment.throwsException = exceptionThrow
        }

        // builds Iterates section
        rootSentenceBlock.iterationSentenceBlocks.forEach { (loopDesc, sentenceBlocks) ->
            customJavaDocComment.iterates += stringTemplates.iterationSentence.format(
                stringTemplates.codeSentence.format(loopDesc),
                numberOccurrencesToText(
                    sentenceBlocks.size
                )
            )
        }

        // builds Invoke, Execute, Return sections
        generateSequence(rootSentenceBlock) { it.nextBlock }.forEach {
            for (statement in it.stmtTexts) {
                when (statement.stmtType) {
                    StmtType.Invoke -> customJavaDocComment.invokes += "{@code ${statement.description}}"
                    StmtType.Condition -> customJavaDocComment.executesCondition += "{@code ${statement.description}}"
                    StmtType.Return -> customJavaDocComment.returnsFrom = "{@code ${statement.description}}"
                    else -> {
                        //TODO: see [issue-773](https://github.com/UnitTestBot/UTBotJava/issues/773)
                    }
                }
            }
        }

        return customJavaDocComment
    }
}