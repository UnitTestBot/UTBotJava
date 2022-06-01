package org.utbot.predictors

import org.utbot.analytics.IUtBotSatPredictor
import org.utbot.analytics.UtBotAbstractPredictor
import org.utbot.engine.pc.UtExpression
import org.utbot.engine.pc.UtSolverStatusKind

@Suppress("unused")
class UtBotSatPredictor : UtBotAbstractPredictor<Iterable<UtExpression>, UtSolverStatusKind>,
        IUtBotSatPredictor<Iterable<UtExpression>> {

    override fun provide(input: Iterable<UtExpression>, expectedResult: UtSolverStatusKind, actualResult: UtSolverStatusKind) {
    }

    override fun terminate() {
    }
}