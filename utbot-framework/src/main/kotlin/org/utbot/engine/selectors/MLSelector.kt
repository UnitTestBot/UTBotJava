package org.utbot.engine.selectors

import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.analytics.Predictors
import org.utbot.engine.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.selectors.nurs.GreedySearch
import org.utbot.engine.selectors.strategies.ChoosingStrategy
import org.utbot.engine.selectors.strategies.GeneratedTestCountingStatistics
import org.utbot.engine.selectors.strategies.StoppingStrategy

/**
 * @see <a href=https://files.sri.inf.ethz.ch/website/papers/ccs21-learch.pdf>Learch</a>
 *
 * Calculates reward using neural network, when state is offered, and then peeks state with maximum reward
 *
 * @see choosingStrategy [ChoosingStrategy] for [GreedySearch]
 *
 * [GreedySearch]
 */
abstract class MLSelector(
    protected val generatedTestCountingStatistics: GeneratedTestCountingStatistics,
    choosingStrategy: ChoosingStrategy,
    stoppingStrategy: StoppingStrategy,
    seed: Int = 42,
    graph: InterProceduralUnitGraph
) : GreedySearch(choosingStrategy, stoppingStrategy, seed) {
    protected val featureExtractor = EngineAnalyticsContext.featureExtractorFactory(graph)

    override val name: String
        get() = "NNRewardGuidedSelector"
}

/**
 * Calculate weight of execution state only when it is offered. It has advantage, because it works faster,
 * than with recalculation but disadvantage is that some features of execution state can change.
 */
class MLSelectorWithoutWeightsRecalculation(
    generatedTestCountingStatistics: GeneratedTestCountingStatistics,
    choosingStrategy: ChoosingStrategy,
    stoppingStrategy: StoppingStrategy,
    seed: Int = 42,
    graph: InterProceduralUnitGraph
) : MLSelector(generatedTestCountingStatistics, choosingStrategy, stoppingStrategy, seed, graph) {
    override fun offerImpl(state: ExecutionState) {
        super.offerImpl(state)
        featureExtractor.extractFeatures(state, generatedTestCountingStatistics.generatedTestsCount)
    }

    override val ExecutionState.weight: Double
        get() {
            reward = reward ?: Predictors.stateRewardPredictor.predict(features)
            return reward as Double
        }
}

/**
 * Calculate weight of execution state every time when it needed. It works slower,
 * than without recalculation but features are always relevant
 */
class MLSelectorWithWeightsRecalculation(
    generatedTestCountingStatistics: GeneratedTestCountingStatistics,
    choosingStrategy: ChoosingStrategy,
    stoppingStrategy: StoppingStrategy,
    seed: Int = 42,
    graph: InterProceduralUnitGraph
) : MLSelector(generatedTestCountingStatistics, choosingStrategy, stoppingStrategy, seed, graph) {
    override val ExecutionState.weight: Double
        get() {
            featureExtractor.extractFeatures(this, generatedTestCountingStatistics.generatedTestsCount)
            return Predictors.stateRewardPredictor.predict(features)
        }
}
