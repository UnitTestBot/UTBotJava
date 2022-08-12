package org.utbot

import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.predictors.StateRewardPredictorFactoryImpl

object AnalyticsSetUp2 {
    init {
        EngineAnalyticsContext.stateRewardPredictorFactory[1] = StateRewardPredictorFactoryImpl()
    }
}