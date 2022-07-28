package org.utbot.summary.comment

import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.exceptionOrNull
import org.utbot.summary.ast.JimpleToASTMap
import org.utbot.summary.tag.TraceTagWithoutExecution
import soot.SootMethod

//TODO: polish code
class CustomJavaDocCommentBuilder(
    traceTag: TraceTagWithoutExecution,
    sootToAST: MutableMap<SootMethod, JimpleToASTMap>
) : SimpleCommentBuilder(traceTag, sootToAST, stringTemplates = StringsTemplatesPlural()) {

    /**
     * Collects statements for final JavaDoc comment.
     */
    fun buildDocStatements(method: SootMethod): List<DocStatement> {
        val comment: CustomJavaDocComment = buildCustomJavaDocComment(method)
        val docStatementList = mutableListOf<DocStatement>()

        docStatementList += DocRegularStmt("@utbot.classUnderTest ${comment.classUnderTest}\n")
        docStatementList += DocRegularStmt("@utbot.methodUnderTest ${comment.methodUnderTest}\n")

        if (comment.expectedResult.isNotEmpty())
            docStatementList += DocRegularStmt("@utbot.expectedResult ${comment.expectedResult}\n")
        if (comment.actualResult.isNotEmpty())
            docStatementList += DocRegularStmt("@utbot.actualResult ${comment.actualResult}\n")
        if (comment.executesCondition.isNotEmpty()) {
            val statement =
                "@utbot.executesCondition ${comment.executesCondition.joinToString(separator = ",\n")}\n"
            docStatementList += DocRegularStmt(statement)
        }
        if (comment.invokes.isNotEmpty()) {
            val statement = "@utbot.invokes ${comment.invokes.joinToString(separator = ",\n")}\n"
            docStatementList += DocRegularStmt(statement)
        }
        if (comment.iterates.isNotEmpty()) {
            val statement = "@utbot.iterates ${comment.iterates.joinToString(separator = ",\n")}\n"
            docStatementList += DocRegularStmt(statement)
        }
        if (comment.returnsFrom.isNotEmpty())
            docStatementList += DocRegularStmt("@utbot.returnsFrom ${comment.returnsFrom}\n")
        if (comment.throwsException.isNotEmpty())
            docStatementList += DocRegularStmt("@utbot.throwsException ${comment.throwsException}")

        return listOf<DocStatement>(DocPreTagStatement(docStatementList))
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
            val exceptionName = thrownException.javaClass.simpleName
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
        var currentBlock: SimpleSentenceBlock? = rootSentenceBlock
        while (currentBlock != null) {
            for (statement in currentBlock.stmtTexts) {
                when (statement.stmtType) {
                    StmtType.Invoke -> {
                        val info = statement.description
                        customJavaDocComment.invokes += "{@code $info}"
                    }
                    StmtType.Condition -> {
                        val info = statement.description
                        customJavaDocComment.executesCondition += "{@code $info}"
                    }
                    StmtType.Return -> {
                        val info = statement.description
                        customJavaDocComment.returnsFrom = "{@code $info}"
                    }
                    else -> {
                        //TODO
                    }
                }
            }
            currentBlock = currentBlock.nextBlock
        }

        return customJavaDocComment
    }
}