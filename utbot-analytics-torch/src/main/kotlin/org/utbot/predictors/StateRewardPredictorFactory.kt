package org.utbot.predictors

import org.utbot.analytics.StateRewardPredictorFactory
import org.utbot.framework.UtSettings

/**
 * Creates [StateRewardPredictor], by checking the [UtSettings] configuration.
 */
class StateRewardPredictorWithTorchModelsSupportFactoryImpl : StateRewardPredictorFactory {
    override operator fun invoke() = StateRewardPredictorTorch()
}