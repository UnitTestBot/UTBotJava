package org.utbot.engine.selectors.strategies

import org.utbot.engine.state.Edge
import org.utbot.engine.state.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.pathLogger
import org.utbot.framework.UtSettings.enableLoggingForDroppedStates
import soot.jimple.Stmt
import soot.jimple.internal.JReturnStmt
import soot.jimple.internal.JReturnVoidStmt
import soot.jimple.internal.JThrowStmt
import soot.toolkits.graph.ExceptionalUnitGraph

/**
 * Counts how many times edges were visited during graph traverse
 */
class EdgeVisitCountingStatistics(
    graph: InterProceduralUnitGraph,
    private val visitNumberToUpdate: Int = 100
) : TraverseGraphStatistics(graph), ChoosingStrategy {
    val visitCounter = mutableMapOf<Edge, Int>()
    private var onVisitedCounter = 0

    /**
     * @return true if execution in this ExecutionState is complete
     */
    private fun ExecutionState.isComplete(): Boolean {
        return !isInNestedMethod() && (stmt is JReturnStmt || stmt is JReturnVoidStmt || stmt is JThrowStmt || isThrowException)
    }

    /**
     * Suggest to drop current executionState if it cannot reach new coverage:
     * - all statements are already covered and execution is complete
     */
    override fun shouldDrop(state: ExecutionState): Boolean {
        val shouldDrop = state.edges.all { graph.isCoveredWithAllThrowStatements(it) } && state.isComplete()

        if (shouldDrop) {
            if (enableLoggingForDroppedStates) {
                pathLogger.debug {
                    val lastStatus = state.solver.lastStatus
                    val md5 = state.md5()
                    "Dropping state (lastStatus=$lastStatus) by the edge visit counting statistics. MD5: $md5"
                }
            }
        }

        return shouldDrop
    }

    /**
     * Adjust counter for visited edge and notify observers every visitNumberToUpdate times
     */
    override fun onVisit(edge: Edge) {
        visitCounter.compute(edge) { _, value ->
            (value ?: 0) + 1
        }
        if (onVisitedCounter++ == visitNumberToUpdate) {
            notifyObservers()
        }
    }

    override fun onJoin(stmt: Stmt, graph: ExceptionalUnitGraph, shouldRegister: Boolean) {
        notifyObservers()
    }

    override fun onTraversed(executionState: ExecutionState) {
        notifyObservers()
    }
}