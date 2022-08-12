package org.utbot

import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.predictors.StateRewardPredictorFactoryImpl

class AnalyticsSetUp {
    companion object {
        init {
            EngineAnalyticsContext.stateRewardPredictorFactory[1] = StateRewardPredictorFactoryImpl()
        }
    }
}