package org.utbot.analytics

import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.selectors.NNRewardGuidedSelectorFactory
import org.utbot.engine.selectors.NNRewardGuidedSelectorWithRecalculationFactory
import org.utbot.engine.selectors.NNRewardGuidedSelectorWithoutRecalculationFactory
import org.utbot.framework.NNRewardGuidedSelectorType
import org.utbot.framework.UtSettings

/**
 * Class that stores all objects that need for work analytics module during symbolic execution
 */
object EngineAnalyticsContext {
    var featureProcessorFactory: FeatureProcessorFactory = object : FeatureProcessorFactory {
        override fun invoke(graph: InterProceduralUnitGraph): FeatureProcessor {
            error("Feature processor factory is not provided.")
        }
    }

    var featureExtractorFactory: FeatureExtractorFactory = object : FeatureExtractorFactory {
        override fun invoke(graph: InterProceduralUnitGraph): FeatureExtractor {
            error("Feature extractor factory is not provided.")
        }
    }

    val nnRewardGuidedSelectorFactory: NNRewardGuidedSelectorFactory = when (UtSettings.nnRewardGuidedSelectorType) {
        NNRewardGuidedSelectorType.WITHOUT_RECALCULATION -> NNRewardGuidedSelectorWithoutRecalculationFactory()
        NNRewardGuidedSelectorType.WITH_RECALCULATION -> NNRewardGuidedSelectorWithRecalculationFactory()
    }

    var stateRewardPredictorFactory: StateRewardPredictorFactory = object : StateRewardPredictorFactory {
        override fun invoke(): StateRewardPredictor {
            error("NNStateRewardPredictor factory wasn't provided")
        }
    }
}