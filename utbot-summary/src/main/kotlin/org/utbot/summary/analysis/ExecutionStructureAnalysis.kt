package org.utbot.summary.analysis

import org.utbot.summary.InvokeDescription
import org.utbot.summary.TraceTagCluster
import org.utbot.summary.jimpleBody
import org.utbot.summary.tag.BasicTypeTag
import org.utbot.summary.tag.StatementTag
import org.utbot.summary.tag.TraceTag
import soot.SootMethod
import soot.jimple.JimpleBody
import soot.jimple.Stmt
import soot.jimple.internal.JGotoStmt
import soot.jimple.internal.JIfStmt
import soot.jimple.internal.JReturnStmt

const val ASSIGN_DECISION_NUM = 0
const val CALL_DECISION_NUM = -2
//const val RETURN_DECISION_NUM = -1

class ExecutionStructureAnalysis {

    /**
     * Deep structural analyzes of each trace is performed.
     * Recursion and loops are identified, then registered in each trace
     * code statement environment is updated: is it in method under test, or inside of invoke, etc
     */
    fun traceStructuralAnalysis(
        jimpleBody: JimpleBody,
        clusteredTags: List<TraceTagCluster>,
        methodUnderTest: SootMethod,
        invokeDescriptions: List<InvokeDescription>
    ) {
        val traceTags = clusteredTags.map { it.traceTags }
        val stmts = jimpleBody.units.filterIsInstance<Stmt>()
        analyzeForRecursionsAndLoops(jimpleBody, traceTags, methodUnderTest)
        invokeDescriptions.forEach { invokeDescription ->
            analyzeForRecursionsAndLoops(
                invokeDescription.sootMethod.jimpleBody(),
                traceTags,
                methodUnderTest
            )
        }
        updateCodeExecutionEnvironmentTag(traceTags)
        fromTwoInvokesToOneInvoke(traceTags)
        indicesReturns(stmts, traceTags)
    }

    /**
     * Analyze traces with given soot method for recursion and loop occurrences
     * @see findLoops
     * @see checkInvokedMethodForRecursion
     */
    private fun analyzeForRecursionsAndLoops(
        jimpleBody: JimpleBody,
        traceTags: List<List<TraceTag>>,
        methodUnderTest: SootMethod
    ) {
        val stmts = jimpleBody.units.filterIsInstance<Stmt>()
        findLoops(stmts, traceTags)
        checkInvokedMethodForRecursion(methodUnderTest, traceTags)
    }

    /**
     * In every trace find and delete one of two Jimple descriptions of assigned invoke.
     * @see ExecutionStructureAnalysis#fromTwoInvokesToOneInvoke
     */
    private fun fromTwoInvokesToOneInvoke(traceTags: List<List<TraceTag>>) {
        for (clusterTags in traceTags) {
            for (traceTag in clusterTags) {
                fromTwoInvokesToOneInvoke(traceTag.rootStatementTag)
            }
        }
    }

    /**
     * Function finds two descriptions of Jimple invokes that belong to actual single call in source code.
     * Soot translates assigned invokes into two Jimple commands.
     * Ex.: int c = math.plus(a, b) translated ->
     *      call a = math.plus(a, b) // Jimple call invoke Math::plus
     *      assign a = math.plus(a, b) // Jimple assign result of invoke Math::plus
     * Delete of extra invoke:
     *      @see ExecutionStructureAnalysis#skipAssignInvokeStatement
     */
    private fun fromTwoInvokesToOneInvoke(stmtTag: StatementTag?) {
        val nextStmtTag = stmtTag?.next
        if (stmtTag == null || nextStmtTag == null) {
            return
        }
        if (stmtTag.basicTypeTag == BasicTypeTag.Invoke && nextStmtTag.basicTypeTag == BasicTypeTag.Invoke) {
            skipAssignInvokeStatement(stmtTag, nextStmtTag)
        }
        if (stmtTag.basicTypeTag == BasicTypeTag.RecursionAssignment && nextStmtTag.basicTypeTag == BasicTypeTag.RecursionAssignment) {
            skipAssignInvokeStatement(stmtTag, nextStmtTag)
        }
        fromTwoInvokesToOneInvoke(stmtTag.invoke)
        fromTwoInvokesToOneInvoke(stmtTag.recursion)
        stmtTag.iterations.forEach {
            fromTwoInvokesToOneInvoke(it)
        }
        fromTwoInvokesToOneInvoke(stmtTag.next)
    }

    /**
     * Function deletes one of two descriptions of invokes belonging to one actual call.
     */
    private fun skipAssignInvokeStatement(stmtTag: StatementTag, nextStmtTag: StatementTag) {
        val currentStmtTagDecision = stmtTag.step.decision
        val nextStmtTagDecision = nextStmtTag.step.decision
        if (currentStmtTagDecision == CALL_DECISION_NUM && nextStmtTagDecision == ASSIGN_DECISION_NUM) {
            stmtTag.next = nextStmtTag.next
        }
    }

    /**
     * Iterates over all traces,
     * and updates the execution environment tag for each trace
     */
    private fun updateCodeExecutionEnvironmentTag(traceTags: List<List<TraceTag>>) {
        for (clusterTags in traceTags) {
            for (traceTag in clusterTags) {
                traceTag.updateExecutionEnvironmentTag()
            }
        }
    }

    private fun checkInvokedMethodForRecursion(methodUnderTest: SootMethod, traceTags: List<List<TraceTag>>) {
        for (clusterTags in traceTags) {
            for (traceTag in clusterTags) {
                checkInvokedMethodForRecursion(methodUnderTest, traceTag.rootStatementTag)
            }
        }
    }

    /**
     * On the initial statement tag tree built, all invokes are identified,
     * Here, the function checks, some of those invokes:
     *      1. actually recursion
     *      2. Invokes a method which triggers recursion.
     * In case 2, we usually limited by a given depth, the recursion path would be unavailable.
     * So, the statement that triggers recursion without its execution path, is tagged as RecursionAssignment.
     * */
    private fun checkInvokedMethodForRecursion(methodUnderTest: SootMethod, stmtTag: StatementTag?) {
        if (stmtTag == null) return
        val invokeStmtTag = stmtTag.invoke
        val sootInvokeMethod = stmtTag.invokeSootMethod()
        if (invokeStmtTag != null && sootInvokeMethod != null) {
            if (sootInvokeMethod == methodUnderTest) {// Found recursion
                stmtTag.recursion = stmtTag.invoke
                stmtTag.invoke = null
                stmtTag.basicTypeTag = BasicTypeTag.Recursion
                checkInvokedMethodForRecursion(methodUnderTest, stmtTag.recursion)
            } else {
                checkInvokedMethodForRecursion(methodUnderTest, invokeStmtTag)
                checkInvokeAssignmentForRecursionAssignment(sootInvokeMethod, invokeStmtTag)
            }
        }
        for (iterationTag in stmtTag.iterations) {
            checkInvokedMethodForRecursion(methodUnderTest, iterationTag)
        }
        checkInvokedMethodForRecursion(methodUnderTest, stmtTag.next)
    }

    /**
     * This method examine the situation:
     *      Method under test calls a method which calls another method. The last call is recursion but without its execution path.
     * If such method found then it is Recursion Assigment.
     * */
    private fun checkInvokeAssignmentForRecursionAssignment(method: SootMethod, stmtTag: StatementTag) {
        if (stmtTag.basicTypeTag == BasicTypeTag.Invoke && stmtTag.invoke == null && stmtTag.invokeSootMethod() == method) {
            stmtTag.basicTypeTag = BasicTypeTag.RecursionAssignment
        }
        stmtTag.iterations.forEach {
            checkInvokeAssignmentForRecursionAssignment(method, it)
        }
        stmtTag.next?.let {
            checkInvokeAssignmentForRecursionAssignment(method, it)
        }
    }

    /**
     * @param stmts - an ordered list of all statements inside jimple body.
     * Function searches JGotoStmt which is the used for loop instructions.
     * JGotoStmt contains the target statement, consequently from target statement
     * till JGotoStmt is a loop sequence. After identification of such sequence,
     * start index and end index is passed to
     * @see registerIterationAtTraces to register in every trace
     * Indices are needed to further check their order. If they are not in order, it is not a loop.
     */
    private fun findLoops(stmts: List<Stmt>, traceTags: List<List<TraceTag>>) {
        val stmtToIndex = stmts.mapIndexed { index, it -> it to index }.toMap()
        for (stmt in stmts) {
            if (stmt is JGotoStmt) {
                val target = stmt.target
                registerIterationAtTraces(stmtToIndex[target], stmtToIndex[stmt], stmts, traceTags)
            }
        }
    }

    /**
     * @param start - start index loop sequence.
     * @param end - end index loop sequence.
     * @param stmts - an ordered list of statements.
     * Indices order is checked and every trace registers this loop.
     */
    private fun registerIterationAtTraces(
        start: Int?,
        end: Int?,
        stmts: List<Stmt>,
        traceTags: List<List<TraceTag>>
    ) {
        if (start != null && end != null && (end - start > 0)) {
            // iteration
            val iterationStmts = stmts.filterIndexed { i, _ -> (i >= start && i <= end) }
            // check trace tags for this iteration
            val startIteration = stmts[start]
            val endIteration = stmts[end]
            for (clusterTraceTags in traceTags) {
                for (traceTag in clusterTraceTags) {
                    TraceIterationAnalyser(traceTag).analyze(startIteration, endIteration, iterationStmts)
                }
            }
        }
    }

    private fun indicesReturns(stmts: List<Stmt>, traceTags: List<List<TraceTag>>) {
        val returnsToNumber = stmts.filterIsInstance<JReturnStmt>().groupBy { it.toString() }
            .flatMap {
                if (it.value.size > 1)
                    it.value.mapIndexed { index, stmt -> stmt to index + 1 }
                else
                    it.value.mapIndexed { index, stmt -> stmt to index }
            }.toMap()

        traceTags.forEach { clusterTags ->
            clusterTags.forEach { traceTag ->
                traceTag.returnsToNumber = returnsToNumber
            }
        }
    }
}

/**
 * Class analyzes a trace for a particular loop iteration,
 * if it finds that given loop is actually iterated in the trace, then it registers it.
 * Otherwise, it registers as a missed loop inside of the trace.
 */
class TraceIterationAnalyser(val traceTag: TraceTag) {
    private var iterationCounter = 0

    fun analyze(
        startStmt: Stmt,
        endStmt: Stmt,
        iteration: List<Stmt>
    ): Boolean {
        val found = findIteration(traceTag.rootStatementTag, startStmt, endStmt, iteration)
        if (!found) {
            traceTag.registerNoIterationCall(iteration)
        }
        return found
    }

    /**
     * The main algorithm that recursively searches the loop sequence inside of trace.
     * @param stmtTag current statement tag where the start of loop is checked.
     * @param startStmt the first statement tag in the loop
     * @param endStmt the last statement tag in the loop
     * @iteration a list of statements of the loop
     * Algorithm looks if the current statement tag is actually start of iteration:
     *      if not, it continues looking at child tags: invokes, iterations and next tag
     *      if yes, then it tries to find where iteration ends in this trace.
     *          if it finds the end of iteration, then it continues searching from next tag,
     *              in case the next tag is the start of new iteration.
     *          if it didn't find the end of iteration then it continues checking two scenarios:
     *              1. There is a break statement inside of the loop, and ends its analysis when it finds it.
     *              2. There is no iteration at all, we have start iteration statement but
     *                  with no actual iteration. Such cases occurs when we have multiple iteration
     *                  of one loop. Ex.: (start statement), ...,(end iteration, go to Start),
     *                                    (start statement), other statements
     */
    private fun findIteration(
        stmtTag: StatementTag?,
        startStmt: Stmt,
        endStmt: Stmt,
        iteration: List<Stmt>
    ): Boolean {
        var result = false
        val nextStmtTag = stmtTag?.next
        if (stmtTag == null || nextStmtTag == null) return result

        if (nextStmtTag.step.stmt == startStmt) {
            val endStmtTag = findEndIteration(nextStmtTag, endStmt)
            nextStmtTag.basicTypeTag = BasicTypeTag.IterationStart
            if (endStmtTag != null) {   // we have found a complete loop
                stmtTag.iterations.add(nextStmtTag)
                stmtTag.next = endStmtTag.next
                endStmtTag.next = null
                endStmtTag.basicTypeTag = BasicTypeTag.IterationEnd
                iterationCounter++
                result = true
                findIteration(stmtTag, startStmt, endStmt, iteration)
            } else {    // no end loop stmt
                nextStmtTag.next?.let {
                    if (isBreakable(it, iteration)) { //
                        val breakStatement = breakChain(it, iteration)
                        stmtTag.iterations.add(nextStmtTag)
                        stmtTag.next = breakStatement
                        iterationCounter++
                        result = true
                    } else { // it is actually exit from loop.
                        // skip nextStmtTag,
                        // nextStmtTag is a false call of start iteration
                        // next iteration is not executed
                        stmtTag.next = it
                    }
                }
            }
        } else {
            stmtTag.invoke?.let {
                result = result || findIteration(it, startStmt, endStmt, iteration)
            }
            for (tag in stmtTag.iterations)
                result = result || findIteration(tag, startStmt, endStmt, iteration)
            result = result || findIteration(nextStmtTag, startStmt, endStmt, iteration)
        }
        return result
    }

    /*
    * Recursively searches the end of iteration in the trace
    * @param stmtTag current considering statement tag
    * @param endStmt the last stamt in the loop
    * */
    private fun findEndIteration(stmtTag: StatementTag?, endStmt: Stmt): StatementTag? {
        if (stmtTag == null) return null
        if (stmtTag.step.stmt == endStmt) {
            return stmtTag
        }
        return findEndIteration(stmtTag.next, endStmt)
    }

    /**
     * In case, we find a start statement of iteration inside of trace
     * but we couldn't identify the end in the trace,
     * we verify can we declare that this iteration is started but abruptly stopped.
     * It can be verified if the next statement also belongs to iteration, then we verify that iteration started
     * and something done in this iteration but it ended abruptly without proper goto stmt.
     * Then this iteration considered to be breaked in some point of trace. May be not properly put name should be
     * Ex. verifyBrokeLoopInTrace
     * However, the nested loops (loops inside of loops) have intersected instructions.
     * Sometimes JIfStmt and JGotoStmt of one loop comes together with other one or mixed, depending how code is written.
     * Loops instructions may depend on each other, and Soot or Java Compiler can optimize them.
     * Consequently, two iteration statements can contain each others JIfStmt && JGotoStmt instructions.
     * That is why we skip them here. If the next stmts are only JIfStmt and JGotoStmt (they may belong to
     * different loops) then it means they (loops) are both abruptly ended.
     * @param statementTag considering where break should be happened
     */
    private fun isBreakable(statementTag: StatementTag?, iteration: List<Stmt>): Boolean {
        if (statementTag == null) return false
        if (statementTag.step.stmt !in iteration) {
            return false // iteration didn't run (previous statement is a dummy)
        }
        if (statementTag.step.stmt !is JIfStmt && statementTag.step.stmt !is JGotoStmt && statementTag.step.stmt in iteration) {
            return true // the iteration is actually performed, and it should be declared as breaked
        }
        return isBreakable(statementTag.next, iteration)
    }

    /**
     * We found that iteration is abruptly ended,
     * and this function identifies where break happened.
     */
    private fun breakChain(statementTag: StatementTag?, iteration: List<Stmt>): StatementTag? {
        val nextStatementTag = statementTag?.next ?: return null
        return if (nextStatementTag.step.stmt in iteration) {
            breakChain(nextStatementTag, iteration)
        } else {
            statementTag.next = null
            nextStatementTag
        }
    }
}

