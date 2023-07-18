package org.utbot.summary.comment.cluster

import com.github.javaparser.ast.stmt.CatchClause
import com.github.javaparser.ast.stmt.ForStmt
import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.summary.SummarySentenceConstants.CARRIAGE_RETURN
import org.utbot.summary.ast.JimpleToASTMap
import org.utbot.summary.comment.*
import org.utbot.summary.comment.classic.symbolic.*
import org.utbot.summary.comment.customtags.getMethodReferenceForSymbolicTest
import org.utbot.summary.tag.BasicTypeTag
import org.utbot.summary.tag.CallOrderTag
import org.utbot.summary.tag.StatementTag
import org.utbot.summary.tag.TraceTagWithoutExecution
import org.utbot.summary.tag.UniquenessTag
import soot.SootMethod
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInvokeStmt
import soot.jimple.internal.JVirtualInvokeExpr

/**
 * Inherits from SimpleCommentBuilder
 */
class SymbolicExecutionClusterCommentBuilder(
    traceTag: TraceTagWithoutExecution,
    sootToAST: MutableMap<SootMethod, JimpleToASTMap>
) : SimpleCommentBuilder(traceTag, sootToAST, stringTemplates = StringsTemplatesPlural()) {
    /**
     * Builds cluster comment, ignores thrown exceptions
     */
    override fun buildString(currentMethod: SootMethod): String {
        val root = SimpleSentenceBlock(stringTemplates = StringsTemplatesPlural())

        skippedIterations()
        buildSentenceBlock(traceTag.rootStatementTag, root, currentMethod)
        var sentence = toSentence(root)

        if (sentence.isEmpty()) {
            return EMPTY_STRING
        }

        sentence = splitLongSentence(sentence)
        sentence = lastCommaToDot(sentence)

        return "<pre>\n$sentence</pre>".replace(CARRIAGE_RETURN, EMPTY_STRING)
    }

    override fun buildDocStmts(currentMethod: SootMethod): List<DocStatement> {
        val root = SimpleSentenceBlock(stringTemplates = StringsTemplatesPlural())

        skippedIterations()
        buildSentenceBlock(traceTag.rootStatementTag, root, currentMethod)
        val sentence = toDocStmts(root)

        if (sentence.isEmpty()) {
            return emptyList()
        }
//        sentence = splitLongSentence(sentence) //TODO SAT-1309
//        sentence = lastCommaToDot(sentence) //TODO SAT-1309

        val docStatements = toDocStmts(root)
        return listOf<DocStatement>(DocPreTagStatement(docStatements))
    }

    /**
     * Builds sentence blocks as parent one,
     * but ignores few types of statementTag that are considered in SimpleCommentBuilder
     */
    override fun buildSentenceBlock(
        statementTag: StatementTag?,
        sentenceBlock: SimpleSentenceBlock,
        currentMethod: SootMethod
    ) {
        val jimpleToASTMap = sootToAST[currentMethod]
        if (statementTag == null) return
        if (jimpleToASTMap == null) return
        val recursion = statementTag.recursion
        val stmt = statementTag.step.stmt
        val invoke = statementTag.invoke
        var createNextBlock = false

        val localNoIterations = statementTagSkippedIteration(statementTag, currentMethod)
        if (localNoIterations.isNotEmpty()) {
            sentenceBlock.notExecutedIterations = localNoIterations
            methodToNoIterationDescription[currentMethod]?.removeAll(localNoIterations)
        }

        val invokeSootMethod = statementTag.invokeSootMethod()
        var invokeRegistered = false
        if (invoke != null && invokeSootMethod != null) {
            val className = invokeSootMethod.declaringClass.name
            val methodName = invokeSootMethod.name
            val methodParameterTypes = invokeSootMethod.parameterTypes
            val isPrivate = invokeSootMethod.isPrivate
            val sentenceInvoke = SimpleSentenceBlock(stringTemplates = StringsTemplatesPlural())
            buildSentenceBlock(invoke, sentenceInvoke, invokeSootMethod)
            sentenceInvoke.squashStmtText()
            if (!sentenceInvoke.isEmpty()) {
                sentenceBlock.invokeSentenceBlock =
                    Pair(
                        getMethodReferenceForSymbolicTest(className, methodName, methodParameterTypes, isPrivate),
                        sentenceInvoke
                    )
                createNextBlock = true
                invokeRegistered = true
            }
        }
        if (statementTag.basicTypeTag == BasicTypeTag.Invoke && statementTag.uniquenessTag == UniquenessTag.Unique && !invokeRegistered) {
            if (statementTag.executionFrequency <= 1) {
                addTextInvoke(sentenceBlock, stmt, statementTag.executionFrequency)
            }
            if (statementTag.executionFrequency > 1 && statementTag.callOrderTag == CallOrderTag.First) {
                addTextInvoke(sentenceBlock, stmt, statementTag.executionFrequency)
            }
        }

        if (statementTag.basicTypeTag == BasicTypeTag.RecursionAssignment && statementTag.uniquenessTag == UniquenessTag.Unique && !invokeRegistered) {
            if (statementTag.executionFrequency <= 1) {
                addTextRecursion(sentenceBlock, stmt, statementTag.executionFrequency)
            }
            if (statementTag.executionFrequency > 1 && statementTag.callOrderTag == CallOrderTag.First) {
                addTextRecursion(sentenceBlock, stmt, statementTag.executionFrequency)
            }
        }

        if (jimpleToASTMap[statementTag.step.stmt] !is ForStmt) {

            if (statementTag.basicTypeTag == BasicTypeTag.Condition && statementTag.callOrderTag == CallOrderTag.First) {
                if (statementTag.uniquenessTag == UniquenessTag.Unique) {
                    val conditionText = textCondition(statementTag, jimpleToASTMap)
                    if (conditionText != null) {
                        sentenceBlock.stmtTexts.add(StmtDescription(StmtType.Condition, conditionText))
                    }
                }
            }

            if (statementTag.basicTypeTag == BasicTypeTag.SwitchCase && statementTag.uniquenessTag == UniquenessTag.Unique) {
                textSwitchCase(statementTag.step, jimpleToASTMap)
                    ?.let { description ->
                        sentenceBlock.stmtTexts.add(StmtDescription(StmtType.SwitchCase, description))
                    }
            }
            if (statementTag.basicTypeTag == BasicTypeTag.CaughtException && statementTag.uniquenessTag == UniquenessTag.Unique) {
                jimpleToASTMap[stmt].let {
                    if (it is CatchClause) {
                        sentenceBlock.stmtTexts.add(StmtDescription(StmtType.CaughtException, it.parameter.toString()))
                    }
                }
            }
            if (statementTag.basicTypeTag == BasicTypeTag.Return && statementTag.uniquenessTag == UniquenessTag.Unique) {
                textReturn(statementTag, sentenceBlock, stmt, jimpleToASTMap)
            }
        }

        if (statementTag.iterations.isNotEmpty()) {
            val iterationSentenceBlock = buildIterationsBlock(statementTag.iterations, statementTag.step, currentMethod)
            sentenceBlock.iterationSentenceBlocks.add(iterationSentenceBlock)
            createNextBlock = true
        }

        if (recursion != null) {
            if (stmt is JAssignStmt) {
                val name = (stmt.rightOp as JVirtualInvokeExpr).method.name
                val sentenceRecursionBlock = SimpleSentenceBlock(stringTemplates = StringsTemplatesPlural())
                buildSentenceBlock(recursion, sentenceRecursionBlock, currentMethod)
                sentenceBlock.recursion = Pair(name, sentenceRecursionBlock)
                createNextBlock = true
            }
            if (stmt is JInvokeStmt) {
                val name = stmt.invokeExpr.method.name
                val sentenceRecursion = SimpleSentenceBlock(stringTemplates = StringsTemplatesPlural())
                buildSentenceBlock(recursion, sentenceRecursion, currentMethod)
                sentenceBlock.recursion = Pair(name, sentenceRecursion)
                createNextBlock = true
            }
        }

        if (createNextBlock) {
            val nextSentenceBlock = SimpleSentenceBlock(stringTemplates = StringsTemplatesPlural())
            sentenceBlock.nextBlock = nextSentenceBlock
            buildSentenceBlock(statementTag.next, nextSentenceBlock, currentMethod)
        } else {
            buildSentenceBlock(statementTag.next, sentenceBlock, currentMethod)
        }
    }
}