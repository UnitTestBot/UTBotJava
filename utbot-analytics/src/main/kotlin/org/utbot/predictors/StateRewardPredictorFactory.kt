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
        StateRewardPredictorType.TORCH -> StateRewardPredictorTorch()
        StateRewardPredictorType.LINEAR -> LinearStateRewardPredictor()
    }
}