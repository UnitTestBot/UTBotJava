package org.utbot.engine.selectors.strategies

import org.utbot.engine.Edge
import org.utbot.engine.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph
import soot.jimple.Stmt
import soot.toolkits.graph.ExceptionalUnitGraph

/**
 * Class that represents callbacks for following events in [InterProceduralUnitGraph]:
 * - joining graph of new method
 * - visiting new edge
 * - successful traverse of path in ExecutionState
 */
abstract class TraverseGraphStatistics(val graph: InterProceduralUnitGraph) {
    init {
        graph.attach(this)
    }

    private val observers = mutableListOf<StrategyObserver>()

    protected fun notifyObservers() {
        observers.forEach { observer ->
            observer.update()
        }
    }

    /**
     * traverse(ExecutionState) callback of listened graph.
     *
     */
    open fun onTraversed(executionState: ExecutionState) {
        // do nothing by default
    }

    /**
     * visit(Edge) callback of listened graph.
     */
    open fun onVisit(edge: Edge) {
        // do nothing by default
    }

    /**
     * visit(Stmt) callback of listened graph.
     */
    open fun onVisit(executionState: ExecutionState) {
        // do nothing by default
    }

    /**
     * join(Stmt,ExceptionalUnitGraph) callback of listened graph.
     */
    open fun onJoin(stmt: Stmt, graph: ExceptionalUnitGraph, shouldRegister: Boolean) {
        // do nothing by default
    }

    /**
     *
     */
    fun subscribe(strategyObserver: StrategyObserver) {
        observers += strategyObserver
    }

}