package org.utbot.summary.comment.classic.symbolic

import com.github.javaparser.ast.stmt.CatchClause
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.SwitchStmt
import com.github.javaparser.ast.stmt.ThrowStmt
import com.github.javaparser.ast.stmt.SwitchEntry
import org.utbot.framework.plugin.api.ArtificialError
import org.utbot.framework.plugin.api.InstrumentedProcessDeathException
import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.Step
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.exceptionOrNull
import org.utbot.summary.AbstractTextBuilder
import org.utbot.summary.NodeConverter
import org.utbot.summary.SummarySentenceConstants.CARRIAGE_RETURN
import org.utbot.summary.ast.JimpleToASTMap
import org.utbot.summary.comment.*
import org.utbot.summary.comment.customtags.getMethodReferenceForSymbolicTest
import org.utbot.summary.tag.BasicTypeTag
import org.utbot.summary.tag.CallOrderTag
import org.utbot.summary.tag.StatementTag
import org.utbot.summary.tag.TraceTagWithoutExecution
import org.utbot.summary.tag.UniquenessTag
import soot.SootMethod
import soot.Type
import soot.jimple.Stmt
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInvokeStmt
import soot.jimple.internal.JVirtualInvokeExpr

private const val JVM_CRASH_REASON = "JVM crash"
const val EMPTY_STRING = ""

open class SimpleCommentBuilder(
    traceTag: TraceTagWithoutExecution,
    sootToAST: MutableMap<SootMethod, JimpleToASTMap>,
    val stringTemplates: StringsTemplatesInterface = StringsTemplatesSingular()
) :
    AbstractTextBuilder(traceTag, sootToAST) {

    /**
     * Creates String from SimpleSentenceBlock
     */
    open fun buildString(currentMethod: SootMethod): String {
        val root = SimpleSentenceBlock(stringTemplates = stringTemplates)
        buildThrownExceptionInfo(root, currentMethod)
        skippedIterations()
        buildSentenceBlock(traceTag.rootStatementTag, root, currentMethod)
        var sentence = toSentence(root)

        if (sentence.isEmpty()) {
            return EMPTY_STRING
        }

        sentence = splitLongSentence(sentence)
        sentence = lastCommaToDot(sentence)

        return "<pre>\n$sentence</pre>".replace(CARRIAGE_RETURN, "")
    }

    private fun buildThrownExceptionInfo(
        root: SimpleSentenceBlock,
        currentMethod: SootMethod
    ) {
        traceTag.result.exceptionOrNull()?.let {
            val exceptionName = it.javaClass.simpleName
            val reason = findExceptionReason(currentMethod, it)

            when (it) {
                is TimeoutException,
                is ArtificialError -> root.detectedError = reason
                else -> root.exceptionThrow = "$exceptionName $reason"
            }
        }
    }

    /**
     * Creates List<[DocStatement]> from [SimpleSentenceBlock].
     */
    open fun buildDocStmts(currentMethod: SootMethod): List<DocStatement> {
        val sentenceBlock = buildSentenceBlock(currentMethod)
        val docStmts = toDocStmts(sentenceBlock)

        if (docStmts.isEmpty()) {
            return emptyList()
        }
//        sentence = splitLongSentence(sentence) //TODO SAT-1309
//        sentence = lastCommaToDot(sentence) //TODO SAT-1309

        return listOf<DocStatement>(DocPreTagStatement(docStmts))
    }

    private fun buildSentenceBlock(currentMethod: SootMethod): SimpleSentenceBlock {
        val rootSentenceBlock = SimpleSentenceBlock(stringTemplates = stringTemplates)
        buildThrownExceptionInfo(rootSentenceBlock, currentMethod)
        skippedIterations()
        buildSentenceBlock(traceTag.rootStatementTag, rootSentenceBlock, currentMethod)
        return rootSentenceBlock
    }

    /**
     * Transforms rootSentenceBlock into String
     */
    protected fun toSentence(rootSentenceBlock: SimpleSentenceBlock): String {
        rootSentenceBlock.squashStmtText()
        val buildSentence = rootSentenceBlock.toSentence()
        if (buildSentence.isEmpty()) return ""
        return "${stringTemplates.sentenceBeginning} $buildSentence"
    }

    /**
     * Transforms rootSentenceBlock into List<DocStatement>
     */
    protected fun toDocStmts(rootSentenceBlock: SimpleSentenceBlock): List<DocStatement> {
        val stmts = mutableListOf<DocStatement>()

        rootSentenceBlock.squashStmtText()
        stmts += rootSentenceBlock.toDocStmt()
        if (stmts.isEmpty()) return emptyList()

        stmts.add(0, DocRegularStmt("${stringTemplates.sentenceBeginning} "))
        return stmts
    }

    protected fun findExceptionReason(currentMethod: SootMethod, thrownException: Throwable): String {
        val path = traceTag.path
        if (path.isEmpty()) {
            if (thrownException is InstrumentedProcessDeathException) {
                return JVM_CRASH_REASON
            }

            error("Cannot find last path step for exception $thrownException")
        }

        return findExceptionReason(path.last(), currentMethod)
    }

    /**
     * Tries to find AST node where the exception was thrown
     * or AST node that contains a condition which lead to the throw.
     */
    private fun findExceptionReason(step: Step, currentMethod: SootMethod): String =
        StringBuilder()
            .let { stringBuilder ->
                val jimpleToASTMap = sootToAST[currentMethod] ?: return ""

                val exceptionNode =
                    jimpleToASTMap.stmtToASTNode[step.stmt]
                        .let { node ->
                            if (node is ThrowStmt) getExceptionReasonForComment(node)
                            else node
                        }
                        ?.also { node ->
                            stringBuilder.append(
                                if (node is IfStmt || node is SwitchEntry) "when: "
                                else "in: "
                            )
                        }
                        ?: return ""

                stringBuilder
                    .append(
                        when {
                            exceptionNode is IfStmt -> exceptionNode.condition.toString()
                            exceptionNode is SwitchEntry -> NodeConverter.convertSwitchEntry(exceptionNode, step, removeSpaces = false)
                            exceptionNode is SwitchStmt -> NodeConverter.convertSwitchStmt(exceptionNode, step, removeSpaces = false)
                            isLoopStatement(exceptionNode) -> getTextIterationDescription(exceptionNode)
                            else -> exceptionNode.toString()
                        }
                    )
            }
            .toString()
            .replace(CARRIAGE_RETURN, "")

    /**
     * Sentence blocks are built based on unique and partly unique statement tags.
     */
    open fun buildSentenceBlock(
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
            val sentenceInvoke = SimpleSentenceBlock(stringTemplates = sentenceBlock.stringTemplates)
            buildSentenceBlock(invoke, sentenceInvoke, invokeSootMethod)
            sentenceInvoke.squashStmtText()
            if (!sentenceInvoke.isEmpty()) {
                sentenceBlock.invokeSentenceBlock =
                    Pair(
                        getMethodReferenceForSymbolicTest(className, methodName, methodParameterTypes, invokeSootMethod.isPrivate),
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
                if (statementTag.uniquenessTag == UniquenessTag.Partly && statementTag.executionFrequency == 1) {
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
            if (statementTag.basicTypeTag == BasicTypeTag.CaughtException) {
                jimpleToASTMap[stmt].let {
                    if (it is CatchClause) {
                        sentenceBlock.stmtTexts.add(StmtDescription(StmtType.CaughtException, it.parameter.toString()))
                    }
                }
            }
            if (statementTag.basicTypeTag == BasicTypeTag.Return) {
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
                val sentenceRecursionBlock = SimpleSentenceBlock(stringTemplates = stringTemplates)
                buildSentenceBlock(recursion, sentenceRecursionBlock, currentMethod)
                sentenceBlock.recursion = Pair(name, sentenceRecursionBlock)
                createNextBlock = true
            }
            if (stmt is JInvokeStmt) {
                val name = stmt.invokeExpr.method.name
                val sentenceRecursion = SimpleSentenceBlock(stringTemplates = stringTemplates)
                buildSentenceBlock(recursion, sentenceRecursion, currentMethod)
                sentenceBlock.recursion = Pair(name, sentenceRecursion)
                createNextBlock = true
            }
        }

        if (createNextBlock) {
            val nextSentenceBlock = SimpleSentenceBlock(stringTemplates = stringTemplates)
            sentenceBlock.nextBlock = nextSentenceBlock
            buildSentenceBlock(statementTag.next, nextSentenceBlock, currentMethod)
        } else {
            buildSentenceBlock(statementTag.next, sentenceBlock, currentMethod)
        }
    }

    /**
     * Adds RecursionAssignment into sentenceBlock.stmtTexts
     */
    protected fun addTextRecursion(sentenceBlock: SimpleSentenceBlock, stmt: Stmt, frequency: Int) {
        if (stmt is JAssignStmt || stmt is JInvokeStmt) {
            val className = stmt.invokeExpr.methodRef.declaringClass.name
            val methodName = stmt.invokeExpr.method.name
            addTextRecursion(sentenceBlock, className, methodName, frequency)
        }
    }

    /**
     * Adds Invoke into sentenceBlock.stmtTexts
     */
    protected fun addTextInvoke(sentenceBlock: SimpleSentenceBlock, stmt: Stmt, frequency: Int) {
        if (stmt is JAssignStmt || stmt is JInvokeStmt) {
            val className = stmt.invokeExpr.methodRef.declaringClass.name
            val methodName = stmt.invokeExpr.method.name
            val methodParameterTypes = stmt.invokeExpr.method.parameterTypes
            val isPrivate = stmt.invokeExpr.method.isPrivate
            addTextInvoke(
                sentenceBlock,
                className,
                methodName,
                methodParameterTypes,
                isPrivate,
                frequency
            )
        }
    }

    /**
     * Adds Invoke into sentenceBlock.stmtTexts
     */
    protected fun addTextInvoke(
        sentenceBlock: SimpleSentenceBlock,
        className: String,
        methodName: String,
        methodParameterTypes: List<Type>,
        isPrivate: Boolean,
        frequency: Int
    ) {
        if (!shouldSkipInvoke(className, methodName))
            sentenceBlock.stmtTexts.add(
                StmtDescription(
                    StmtType.Invoke,
                    getMethodReferenceForSymbolicTest(className, methodName, methodParameterTypes, isPrivate),
                    frequency
                )
            )
    }

    /**
     * Adds RecursionAssignment into sentenceBlock.stmtTexts
     */
    protected fun addTextRecursion(
        sentenceBlock: SimpleSentenceBlock,
        className: String,
        methodName: String,
        frequency: Int
    ) {
        if (!shouldSkipInvoke(className, methodName))
            sentenceBlock.stmtTexts.add(
                StmtDescription(
                    StmtType.RecursionAssignment,
                    methodName,
                    frequency
                )
            )
    }

    protected fun buildIterationsBlock(
        iterations: List<StatementTag>,
        activatedStep: Step,
        currentMethod: SootMethod
    ): Pair<String, List<SimpleSentenceBlock>> {
        val result = mutableListOf<SimpleSentenceBlock>()
        val jimpleToASTMap = sootToAST[currentMethod]
        iterations.forEach {
            val sentenceBlock = SimpleSentenceBlock(stringTemplates = stringTemplates)
            buildSentenceBlock(it, sentenceBlock, currentMethod)
            result.add(sentenceBlock)
        }
        val firstStmtNode = jimpleToASTMap?.get(iterations.first().step.stmt)
        val activatedNode = jimpleToASTMap?.get(activatedStep.stmt)
        val line = iterations.first().line

        var iterationDescription = ""
        if (firstStmtNode is Statement) {
            iterationDescription = getTextIterationDescription(firstStmtNode)
        }
        // getTextIterationDescription can return empty description,
        // that is why if else is not used here.
        if (iterationDescription.isEmpty() && activatedNode is Statement) {
            iterationDescription = getTextIterationDescription(activatedNode)
        }
        //heh, we are still looking for loop txt
        if (iterationDescription.isEmpty()) {
            val nearestNode = jimpleToASTMap?.nearestIterationNode(activatedNode, line)
            if (nearestNode != null) {
                iterationDescription = getTextIterationDescription(nearestNode)
            }
        }

        if (iterationDescription.isEmpty()) {
            val nearestNode = jimpleToASTMap?.nearestIterationNode(firstStmtNode, line)
            if (nearestNode != null) {
                iterationDescription = getTextIterationDescription(nearestNode)
            }
        }

        return Pair(iterationDescription, result)
    }
}

data class IterationDescription(val from: Int, val to: Int, val description: String, val typeDescription: String)
