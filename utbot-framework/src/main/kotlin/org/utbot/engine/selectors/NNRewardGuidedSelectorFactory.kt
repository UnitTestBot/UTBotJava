package org.utbot.engine.selectors

import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.GeneratedTestCountingStatistics
import org.utbot.engine.selectors.strategies.StoppingStrategy

/**
 * Creates [NNRewardGuidedSelector]
 */
interface NNRewardGuidedSelectorFactory {
    operator fun invoke(
        generatedTestCountingStatistics: GeneratedTestCountingStatistics,
        choosingStrategy: ChoosingStrategy,
        stoppingStrategy: StoppingStrategy,
        seed: Int = 42,
        graph: InterProceduralUnitGraph
    ): NNRewardGuidedSelector
}

/**
 * Creates [NNRewardGuidedSelectorWithWeightsRecalculation]
 */
class NNRewardGuidedSelectorWithRecalculationFactory : NNRewardGuidedSelectorFactory {
    override fun invoke(
        generatedTestCountingStatistics: GeneratedTestCountingStatistics,
        choosingStrategy: ChoosingStrategy,
        stoppingStrategy: StoppingStrategy,
        seed: Int,
        graph: InterProceduralUnitGraph
    ): NNRewardGuidedSelector = NNRewardGuidedSelectorWithWeightsRecalculation(
        generatedTestCountingStatistics, choosingStrategy, stoppingStrategy, seed, graph
    )
}

/**
 * Creates [NNRewardGuidedSelectorWithoutWeightsRecalculation]
 */
class NNRewardGuidedSelectorWithoutRecalculationFactory : NNRewardGuidedSelectorFactory {
    override fun invoke(
        generatedTestCountingStatistics: GeneratedTestCountingStatistics,
        choosingStrategy: ChoosingStrategy,
        stoppingStrategy: StoppingStrategy,
        seed: Int,
        graph: InterProceduralUnitGraph
    ): NNRewardGuidedSelector = NNRewardGuidedSelectorWithoutWeightsRecalculation(
        generatedTestCountingStatistics, choosingStrategy, stoppingStrategy, seed, graph
    )
}