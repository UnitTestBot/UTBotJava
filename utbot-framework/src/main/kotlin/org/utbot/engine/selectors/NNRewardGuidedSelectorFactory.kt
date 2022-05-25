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
 * Creates [NNRewardGuidedSelectorWithRecalculationWeight]
 */
class NNRewardGuidedSelectorWithRecalculationFactory : NNRewardGuidedSelectorFactory {
    override fun invoke(
        generatedTestCountingStatistics: GeneratedTestCountingStatistics,
        choosingStrategy: ChoosingStrategy,
        stoppingStrategy: StoppingStrategy,
        seed: Int,
        graph: InterProceduralUnitGraph
    ): NNRewardGuidedSelector = NNRewardGuidedSelectorWithRecalculationWeight(
        generatedTestCountingStatistics, choosingStrategy, stoppingStrategy, seed, graph
    )
}

/**
 * Creates [NNRewardGuidedSelectorWithoutRecalculationWeight]
 */
class NNRewardGuidedSelectorWithoutRecalculationFactory : NNRewardGuidedSelectorFactory {
    override fun invoke(
        generatedTestCountingStatistics: GeneratedTestCountingStatistics,
        choosingStrategy: ChoosingStrategy,
        stoppingStrategy: StoppingStrategy,
        seed: Int,
        graph: InterProceduralUnitGraph
    ): NNRewardGuidedSelector = NNRewardGuidedSelectorWithoutRecalculationWeight(
        generatedTestCountingStatistics, choosingStrategy, stoppingStrategy, seed, graph
    )
}