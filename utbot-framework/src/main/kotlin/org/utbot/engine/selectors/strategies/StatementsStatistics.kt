package org.utbot.engine.selectors.strategies

import org.utbot.engine.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph
import soot.SootMethod
import soot.jimple.Stmt

/**
 * Calculates
 * - number of times for which state’s current instruction has been visited in [statementsCount]
 * - number of instructions visited in state’s current method in [statementsInMethodCount]
 */
class StatementsStatistics(
    graph: InterProceduralUnitGraph
) : TraverseGraphStatistics(graph) {
    private val statementsCount = mutableMapOf<Stmt, Int>()
    private val statementsInMethodCount = mutableMapOf<SootMethod, Int>()

    override fun onVisit(executionState: ExecutionState) {
        statementsCount.compute(executionState.stmt) { _, v ->
            v?.plus(1) ?: 1
        }

        if (statementsCount[executionState.stmt] == 1) {
            executionState.lastMethod?.let {
                statementsInMethodCount.compute(it) { _, v ->
                    v?.plus(1) ?: 1
                }
            }
        }
    }

    fun statementCount(executionState: ExecutionState) = statementsCount.getOrDefault(executionState.stmt, 1)

    fun statementInMethodCount(executionState: ExecutionState) =
        executionState.lastMethod?.let { statementsInMethodCount.getOrDefault(it, 1) } ?: 0
}