package org.utbot.summary.comment

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.CatchClause
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.stmt.SwitchStmt
import com.github.javaparser.ast.stmt.ThrowStmt
import org.utbot.framework.plugin.api.ConcreteExecutionFailureException
import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import org.utbot.framework.plugin.api.Step
import org.utbot.framework.plugin.api.exceptionOrNull
import org.utbot.summary.AbstractTextBuilder
import org.utbot.summary.SummarySentenceConstants.CARRIAGE_RETURN
import org.utbot.summary.ast.JimpleToASTMap
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

        val thrownException = traceTag.result.exceptionOrNull()
        if (thrownException == null) {
            root.exceptionThrow = traceTag.result.exceptionOrNull()?.let { it::class.qualifiedName }
        } else {
            val exceptionName = thrownException.javaClass.simpleName
            val reason = findExceptionReason(currentMethod, thrownException)
            root.exceptionThrow = "$exceptionName $reason"
        }
        skippedIterations()
        buildSentenceBlock(traceTag.rootStatementTag, root, currentMethod)
        var sentence = toSentence(root)
        if (sentence.isEmpty()) return genWarnNotification()
        sentence = splitLongSentence(sentence)
        sentence = lastCommaToDot(sentence)

        return "<pre>\n$sentence</pre>".replace(CARRIAGE_RETURN, "")
    }

    /**
     * Creates List<DocStatement> from SimpleSentenceBlock
     */
    open fun buildDocStmts(currentMethod: SootMethod): List<DocStatement> {
        val root = SimpleSentenceBlock(stringTemplates = stringTemplates)

        val thrownException = traceTag.result.exceptionOrNull()
        if (thrownException == null) {
            root.exceptionThrow = traceTag.result.exceptionOrNull()?.let { it::class.qualifiedName }
        } else {
            val exceptionName = thrownException.javaClass.simpleName
            val reason = findExceptionReason(currentMethod, thrownException)
            root.exceptionThrow = "$exceptionName $reason"
        }
        skippedIterations()
        buildSentenceBlock(traceTag.rootStatementTag, root, currentMethod)
        val docStmts = toDocStmts(root)

        if (docStmts.isEmpty()) {
            return listOf(DocRegularStmt(genWarnNotification())) //TODO SAT-1310
        }
//        sentence = splitLongSentence(sentence) //TODO SAT-1309
//        sentence = lastCommaToDot(sentence) //TODO SAT-1309

        return listOf<DocStatement>(DocPreTagStatement(docStmts))
    }

    protected fun genWarnNotification(): String = " " //why is it empty?

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

    private fun findExceptionReason(currentMethod: SootMethod, thrownException: Throwable): String {
        val path = traceTag.path
        if (path.isEmpty()) {
            if (thrownException is ConcreteExecutionFailureException) {
                return JVM_CRASH_REASON
            }

            error("Cannot find last path step for exception $thrownException")
        }

        return findExceptionReason(path.last(), currentMethod)
    }

    /**
     * Tries to find ast node where exception was thrown
     * or condition if exception was thrown manually in function body
     */
    protected fun findExceptionReason(step: Step, currentMethod: SootMethod): String {
        var reason = ""
        val exceptionStmt = step.stmt
        val jimpleToASTMap = sootToAST[currentMethod] ?: return ""
        var exceptionNode = jimpleToASTMap.stmtToASTNode[exceptionStmt]
        if (exceptionNode is ThrowStmt) {
            exceptionNode = getExceptionReason(exceptionNode)
            reason += "after condition: "
        } else reason += "in: "

        //special case if reason is MethodDeclaration -> exception was thrown after body execution, not after condition
        if (exceptionNode is MethodDeclaration) return "in ${exceptionNode.name} function body"
        //node is SwitchStmt only when jimple stmt is inside selector
        if (exceptionNode is SwitchStmt) exceptionNode = exceptionNode.selector

        if (exceptionNode == null) return ""

        reason += when {
            exceptionNode is IfStmt -> exceptionNode.condition.toString()
            isLoopStatement(exceptionNode) -> getTextIterationDescription(exceptionNode)
            exceptionNode is SwitchStmt -> textSwitchCase(step, jimpleToASTMap)
            else -> exceptionNode.toString()
        }

        return reason.replace(CARRIAGE_RETURN, "")
    }

    /**
     * Sentence blocks are built based on unique and partly unique statement tags.
     */
    private fun buildSentenceBlock(
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
                    Pair(invokeDescription(className, methodName, methodParameterTypes), sentenceInvoke)
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
                val switchCase = textSwitchCase(statementTag.step, jimpleToASTMap)
                if (switchCase != null) {
                    sentenceBlock.stmtTexts.add(StmtDescription(StmtType.SwitchCase, switchCase))
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
                val name = (stmt.rightBox.value as JVirtualInvokeExpr).method.name
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
            val methodName = stmt.invokeExpr.method.name
            addTextRecursion(sentenceBlock, methodName, frequency)
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
            addTextInvoke(sentenceBlock, className, methodName, methodParameterTypes, frequency)
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
        frequency: Int
    ) {
        if (!shouldSkipInvoke(methodName))
            sentenceBlock.stmtTexts.add(
                StmtDescription(
                    StmtType.Invoke,
                    invokeDescription(className, methodName, methodParameterTypes),
                    frequency
                )
            )
    }

    /**
     * Adds RecursionAssignment into sentenceBlock.stmtTexts
     */
    protected fun addTextRecursion(
        sentenceBlock: SimpleSentenceBlock,
        methodName: String,
        frequency: Int
    ) {
        if (!shouldSkipInvoke(methodName))
            sentenceBlock.stmtTexts.add(
                StmtDescription(
                    StmtType.RecursionAssignment,
                    methodName,
                    frequency
                )
            )
    }

    /**
     * Returns a reference to the invoked method.
     *
     * It looks like {@link packageName.className#methodName(type1, type2)}.
     *
     * In case when an enclosing class in nested, we need to replace '$' with '.'
     * to render the reference.
     */
    protected fun invokeDescription(className: String, methodName: String, methodParameterTypes: List<Type>): String {
        val prettyClassName: String = if (className.contains("$")) {
            className.replace("$", ".")
        } else {
            className
        }

        return if (methodParameterTypes.isEmpty()) {
            "{@link $prettyClassName#$methodName()}"
        } else {
            val methodParametersAsString = methodParameterTypes.joinToString(",")
            "{@link $prettyClassName#$methodName($methodParametersAsString)}"
        }
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
