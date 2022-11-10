package org.utbot.engine.taint.priority

import org.utbot.engine.taint.TaintMethodWithTimeout

sealed interface TaintMethodsAnalysisPrioritizer {
    fun sortByPriority(taintMethodsWithTimeout: List<TaintMethodWithTimeout>): List<TaintMethodWithTimeout>
}

object SimpleTaintMethodsAnalysisPrioritizer : TaintMethodsAnalysisPrioritizer {
    override fun sortByPriority(taintMethodsWithTimeout: List<TaintMethodWithTimeout>): List<TaintMethodWithTimeout> =
        taintMethodsWithTimeout
}
