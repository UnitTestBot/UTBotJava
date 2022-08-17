package org.utbot

import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.features.FeatureExtractorFactoryImpl
import org.utbot.features.FeatureProcessorWithStatesRepetitionFactory
import org.utbot.predictors.StateRewardPredictorFactoryImpl

object AnalyticsConfiguration {
    init {
        EngineAnalyticsContext.featureProcessorFactory = FeatureProcessorWithStatesRepetitionFactory()
        EngineAnalyticsContext.featureExtractorFactory = FeatureExtractorFactoryImpl()
        EngineAnalyticsContext.stateRewardPredictorFactory[1] = StateRewardPredictorFactoryImpl()
    }
}