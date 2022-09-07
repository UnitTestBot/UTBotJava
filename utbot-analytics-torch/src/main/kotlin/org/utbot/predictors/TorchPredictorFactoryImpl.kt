package org.utbot.predictors

import org.utbot.analytics.MLPredictorFactory
import org.utbot.framework.UtSettings

/**
 * Creates [StateRewardPredictor], by checking the [UtSettings] configuration.
 */
class TorchPredictorFactoryImpl : MLPredictorFactory {
    override operator fun invoke() = TorchPredictor()
}