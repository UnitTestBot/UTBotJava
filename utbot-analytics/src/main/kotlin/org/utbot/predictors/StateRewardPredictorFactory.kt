package org.utbot.predictors

import org.utbot.analytics.StateRewardPredictorFactory
import org.utbot.framework.StateRewardPredictorType
import org.utbot.framework.UtSettings

/**
 * Creates [StateRewardPredictor], by checking the [UtSettings] configuration.
 */
class StateRewardPredictorFactoryImpl : StateRewardPredictorFactory {
    override operator fun invoke() = when (UtSettings.stateRewardPredictorType) {
        StateRewardPredictorType.BASE -> NNStateRewardPredictorBase()
        StateRewardPredictorType.TORCH -> error("The torch model adapter is bundled with the utbot-analytics-torch module!")
        StateRewardPredictorType.LINEAR -> LinearStateRewardPredictor()
    }
}