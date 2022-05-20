package org.utbot.engine.selectors.nurs

import org.utbot.engine.ExecutionState
import org.utbot.engine.TypeRegistry
import org.utbot.engine.selectors.strategies.DistanceStatistics
import org.utbot.engine.selectors.strategies.StoppingStrategy
import kotlin.math.pow
import soot.jimple.Stmt

class InheritorsSelector(
    override val choosingStrategy: DistanceStatistics,
    stoppingStrategy: StoppingStrategy,
    val typeRegistry: TypeRegistry,
    seed: Int? = 42
) : NonUniformRandomSearch(choosingStrategy, stoppingStrategy, seed) {

    private val sortedInheritors = mutableMapOf<Stmt, List<Stmt>>()

    init {
        choosingStrategy.subscribe(this)
    }

    override val ExecutionState.cost: Double
        get() {
            val distance = choosingStrategy.distanceToUncovered(this).let { d ->
                if (!edges.all { choosingStrategy.isCovered(it) }) {
                    pathLength.toDouble() / (edges.size + 1)
                } else if (d == Int.MAX_VALUE) {
                    Double.MAX_VALUE
                } else {
                    d + pathLength.toDouble() / (edges.size + 1)
                }
            }

            val numberOfStmtOccurrencesInPath = visitedStatementsHashesToCountInPath[stmt.hashCode()] ?: 0

            val distanceWithCycleCoefficient = distance + numberOfStmtOccurrencesInPath * REPEATED_STMT_COEFFICIENT

            // Search and check that the previous stmt is an interface or a virtual invoke
            val caller = path.lastOrNull() ?: return distanceWithCycleCoefficient
            if (!caller.containsInvokeExpr()) return distanceWithCycleCoefficient

            val invokeSuccs = choosingStrategy.graph.invokeSuccessors[caller]
            if (invokeSuccs == null || invokeSuccs.size <= 1) return distanceWithCycleCoefficient

            // Search for all possible inheritors in a graph sorted by their rating
            val ratings = if (sortedInheritors[caller]?.size == invokeSuccs.size) {
                sortedInheritors.getValue(caller)
            } else {
                invokeSuccs.sortedByDescending {
                    typeRegistry.findRating(choosingStrategy.graph.method(it).declaringClass.type).toDouble()
                }.also { sortedInheritors[caller] = it }
            }

            return BASE_FOR_INHERITOR_RATING_COEFFICIENT.pow(ratings.indexOf(stmt)) + distanceWithCycleCoefficient
        }

    override val name = "NURS:Inheritors"

    companion object {
        private const val REPEATED_STMT_COEFFICIENT: Int = 10
        private const val BASE_FOR_INHERITOR_RATING_COEFFICIENT: Double = 100.0
    }
}