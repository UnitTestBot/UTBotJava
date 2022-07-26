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
        if (comment.expectedResult != null)
            docStatementList += DocRegularStmt("@utbot.expectedResult ${comment.expectedResult}\n")
        if (comment.actualResult != null)
            docStatementList += DocRegularStmt("@utbot.actualResult ${comment.actualResult}\n")
        if (comment.executes != null)
            docStatementList += DocRegularStmt("@utbot.executes ${comment.executes}\n")
        if (comment.invokes != null)
            docStatementList += DocRegularStmt("@utbot.invokes ${comment.invokes}\n")
        if (comment.iterates != null)
            docStatementList += DocRegularStmt("@utbot.iterates ${comment.iterates}\n")
        if (comment.returnsFrom != null)
            docStatementList += DocRegularStmt("@utbot.returnsFrom ${comment.returnsFrom}\n")
        if (comment.throwsException != null)
            docStatementList += DocRegularStmt("@utbot.throwsException ${comment.throwsException}")

        return listOf<DocStatement>(DocPreTagStatement(docStatementList))
    }

    private fun buildCustomJavaDocComment(currentMethod: SootMethod): CustomJavaDocComment {
        val methodReference =
            getMethodReference(currentMethod.declaringClass.name, currentMethod.name, currentMethod.parameterTypes)
        val classReference = getClassReference(currentMethod.declaringClass.javaStyleName)

        val thrownException = traceTag.result.exceptionOrNull()
        val exceptionThrow: String? = if (thrownException == null) {
            traceTag.result.exceptionOrNull()?.let { it::class.qualifiedName }
        } else {
            val exceptionName = thrownException.javaClass.simpleName
            val reason = findExceptionReason(currentMethod, thrownException)
            "{@link $exceptionName} $reason"
        }

        val customJavaDocComment = CustomJavaDocComment(
            classUnderTest = classReference,
            methodUnderTest = methodReference,
            expectedResult = null,
            actualResult = null,
            executes = null,
            invokes = null,
            iterates = null,
            returnsFrom = null,
            throwsException = exceptionThrow
        )

        val rootSentenceBlock = SimpleSentenceBlock(stringTemplates = stringTemplates)

        buildSentenceBlock(traceTag.rootStatementTag, rootSentenceBlock, currentMethod)

        for (stmtDescription: StmtDescription in rootSentenceBlock.stmtTexts) {
            when (stmtDescription.stmtType.name) {
                "Invoke" -> {
                    val info = stmtDescription.description
                    customJavaDocComment.invokes = "{@code $info}"
                }
                "Return" -> {
                    val info = stmtDescription.description
                    customJavaDocComment.returnsFrom = "{@code $info}"
                }
                "Condition" -> {
                    val info = stmtDescription.description
                    customJavaDocComment.executes = "{@code $info}"
                }
            }
        }

        return customJavaDocComment
    }
}