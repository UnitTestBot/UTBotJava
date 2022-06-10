package org.utbot.analytics

import org.utbot.engine.pc.UtExpression
import soot.jimple.Stmt

/**
 * Helper class for incremental support
 */
data class IncrementalData(val constraints: Iterable<UtExpression>, val constraintsToAdd: Iterable<UtExpression>)


/**
 * All AI-based predictors and oracles will be located in this global state. Developer can substitute them manually.
 */
object Predictors {
    /**
     * Predict z3's [Solver.check()] execution time in nanoseconds
     */
    var smt: UtBotNanoTimePredictor<Iterable<UtExpression>> = object : UtBotNanoTimePredictor<Iterable<UtExpression>> {}
    var smtIncremental: UtBotNanoTimePredictor<IncrementalData> = object : UtBotNanoTimePredictor<IncrementalData> {}
    var testName: UtBotAbstractPredictor<Iterable<Stmt>, String> =
        object : UtBotAbstractPredictor<Iterable<Stmt>, String> {
            override fun predict(input: Iterable<Stmt>): String = "stubName"
        }

    var stateRewardPredictor: UtBotAbstractPredictor<List<Double>, Double> =
        object : UtBotAbstractPredictor<List<Double>, Double> {
            override fun predict(input: List<Double>): Double {
                error("stateRewardPredictor is not provided.")
            }
        }
}