package org.utbot.summary.tag

import org.utbot.framework.plugin.api.Step
import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.summary.clustering.SplitSteps
import soot.jimple.Stmt
import soot.jimple.internal.JReturnStmt


open class TraceTagWithoutExecution(val path: List<Step>, val result: UtExecutionResult, splitSteps: SplitSteps) {
    var summary: String = ""

    // Tags
    val rootStatementTag = StatementTreeBuilder(splitSteps, path).build()

    // If this execution misses iteration of some loops as while loop, for loops
    // then it is tracked here. Any loop is a set of statements.
    val noIterationCall = mutableListOf<Set<Stmt>>()
    var returnsToNumber: Map<JReturnStmt, Int>? = null

    constructor(execution: UtSymbolicExecution, splitSteps: SplitSteps) : this(execution.path, execution.result, splitSteps)

    /*
    *  If stmt is already contained in the previously registered iteration, it is not registered a second time.
    *  This avoids the situation when there is iteration inside iteration.
    *  Stmts are not registered twice as in inner and outer loop.
    *  Iterations are added consecutively from the most inner loop to most outer loop.
    *  The outer loop usually contains information about inner loop,
    *  it is here filtered out.
    * */
    fun registerNoIterationCall(iteration: List<Stmt>) {
        val registeredStmts = noIterationCall.flatten().toSet()
        noIterationCall.add(iteration.filter { it !in registeredStmts }.toSet())
    }

    /*
    * Updates the tags of the execution environment for each Statement in this structure,
    * begins from root tag which is always executed inside method under test
    */
    fun updateExecutionEnvironmentTag() {
        rootStatementTag?.updateExecutionEnvironmentTag(CodeEnvironment.MUT)
    }
}
