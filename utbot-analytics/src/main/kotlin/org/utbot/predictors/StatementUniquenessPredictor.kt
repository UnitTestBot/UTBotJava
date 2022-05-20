package org.utbot.predictors

import org.utbot.analytics.UtBotAbstractPredictor
import soot.jimple.Stmt

class StatementUniquenessPredictor : UtBotAbstractPredictor<Iterable<Stmt>, String> {

    override fun predict(input: Iterable<Stmt>): String {
        return "<no-actual>"
    }

    override fun provide(input: Iterable<Stmt>, expectedResult: String, actualResult: String) {
    }
}