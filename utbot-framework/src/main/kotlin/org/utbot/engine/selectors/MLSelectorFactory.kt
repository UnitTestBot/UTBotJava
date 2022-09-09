package org.utbot.engine.selectors

import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.GeneratedTestCountingStatistics
import org.utbot.engine.selectors.strategies.StoppingStrategy

/**
 * Creates [MLSelector]
 */
interface MLSelectorFactory {
    operator fun invoke(
        generatedTestCountingStatistics: GeneratedTestCountingStatistics,
        choosingStrategy: ChoosingStrategy,
        stoppingStrategy: StoppingStrategy,
        seed: Int = 42,
        graph: InterProceduralUnitGraph
    ): MLSelector
}

/**
 * Creates [MLSelectorWithWeightsRecalculation]
 */
class MLSelectorWithRecalculationFactory : MLSelectorFactory {
    override fun invoke(
        generatedTestCountingStatistics: GeneratedTestCountingStatistics,
        choosingStrategy: ChoosingStrategy,
        stoppingStrategy: StoppingStrategy,
        seed: Int,
        graph: InterProceduralUnitGraph
    ): MLSelector = MLSelectorWithWeightsRecalculation(
        generatedTestCountingStatistics, choosingStrategy, stoppingStrategy, seed, graph
    )
}

/**
 * Creates [MLSelectorWithoutWeightsRecalculation]
 */
class MLSelectorWithoutRecalculationFactory : MLSelectorFactory {
    override fun invoke(
        generatedTestCountingStatistics: GeneratedTestCountingStatistics,
        choosingStrategy: ChoosingStrategy,
        stoppingStrategy: StoppingStrategy,
        seed: Int,
        graph: InterProceduralUnitGraph
    ): MLSelector = MLSelectorWithoutWeightsRecalculation(
        generatedTestCountingStatistics, choosingStrategy, stoppingStrategy, seed, graph
    )
}