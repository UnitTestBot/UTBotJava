package org.utbot.analytics

import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.selectors.MLSelectorFactory
import org.utbot.engine.selectors.MLSelectorWithRecalculationFactory
import org.utbot.engine.selectors.MLSelectorWithoutRecalculationFactory
import org.utbot.framework.MLSelectorRecalculationType
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

    val mlSelectorFactory: MLSelectorFactory = when (UtSettings.mlSelectorRecalculationType) {
        MLSelectorRecalculationType.WITHOUT_RECALCULATION -> MLSelectorWithoutRecalculationFactory()
        MLSelectorRecalculationType.WITH_RECALCULATION -> MLSelectorWithRecalculationFactory()
    }

    var mlPredictorFactory: MLPredictorFactory = object : MLPredictorFactory {
        override fun invoke(): MLPredictor {
            error("MLPredictor factory wasn't provided")
        }
    }
}