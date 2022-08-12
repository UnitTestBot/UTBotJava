package org.utbot

import org.utbot.analytics.EngineAnalyticsContext
import org.utbot.predictors.StateRewardPredictorFactoryImpl

class AnalyticsSetUp {
    init {
        EngineAnalyticsContext.stateRewardPredictorFactory[1] = StateRewardPredictorFactoryImpl()
    }
    companion object {
        init {
            EngineAnalyticsContext.stateRewardPredictorFactory[1] = StateRewardPredictorFactoryImpl()
        }
    }
}