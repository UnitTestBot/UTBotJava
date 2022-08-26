package org.utbot

import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.features.FeatureExtractorFactoryImpl
import org.utbot.features.FeatureProcessorWithStatesRepetitionFactory
import org.utbot.predictors.StateRewardPredictorFactoryImpl

/**
 * The basic configuration of the utbot-analytics module used in utbot-intellij and (as planned) in utbot-cli
 * to implement the hidden configuration initialization to avoid direct calls of this configuration and usage of utbot-analytics imports.
 *
 * @see <a href="https://github.com/UnitTestBot/UTBotJava/issues/725">
 *     Issue: Enable utbot-analytics module in utbot-intellij module</a>
 */
object AnalyticsConfiguration {
    init {
        EngineAnalyticsContext.featureProcessorFactory = FeatureProcessorWithStatesRepetitionFactory()
        EngineAnalyticsContext.featureExtractorFactory = FeatureExtractorFactoryImpl()
        EngineAnalyticsContext.stateRewardPredictorFactory = StateRewardPredictorFactoryImpl()
    }
}