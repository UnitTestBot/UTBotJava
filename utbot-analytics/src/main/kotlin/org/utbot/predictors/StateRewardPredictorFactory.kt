package org.utbot.predictors

import org.utbot.analytics.MLPredictor
import org.utbot.analytics.MLPredictorFactory
import org.utbot.framework.MLPredictorType
import org.utbot.framework.UtSettings

/**
 * Creates [MLPredictor], by checking the [UtSettings] configuration.
 */
class MLPredictorFactoryImpl : MLPredictorFactory {
    override operator fun invoke() = when (UtSettings.mlPredictorType) {
        MLPredictorType.MLP -> MultilayerPerceptronPredictor()
        MLPredictorType.LINREG -> LinearRegressionPredictor()
    }
}