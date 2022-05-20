package org.utbot.engine.selectors.strategies

import org.utbot.engine.selectors.PathSelector

/**
 * [PathSelector] that can observe updates on [TraverseGraphStatistics]
 */
interface StrategyObserver : PathSelector {
    fun update()
}