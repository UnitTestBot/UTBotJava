package org.utbot.predictors

import mu.KotlinLogging
import org.utbot.framework.PathSelectorType
import org.utbot.framework.UtSettings
import smile.math.matrix.Matrix

private const val DEFAULT_MODEL_PATH = "nn.json"
private const val DEFAULT_SCALER_PATH = "scaler.txt"

private val logger = KotlinLogging.logger {}

private fun getModel(path: String) = buildModel(loadModel(path))

class NNStateRewardPredictorBase(modelPath: String = DEFAULT_MODEL_PATH, scalerPath: String = DEFAULT_SCALER_PATH) :
    NNStateRewardPredictor {
    private lateinit var nn: FeedForwardNetwork
    private lateinit var scaler: StandardScaler

    init {
        try {
            nn = getModel(modelPath)
            scaler = loadScaler(scalerPath)
        } catch (e: Exception) {
            logger.error(e) {
                "Error while initialization of NNStateRewardPredictorBase. Changing pathSelectorType on INHERITORS_SELECTOR"
            }
            UtSettings.pathSelectorType = PathSelectorType.INHERITORS_SELECTOR
        }
    }

    override fun predict(input: List<Double>): Double {
        var inputArray = input.toDoubleArray()
        inputArray = Matrix(inputArray).sub(scaler.mean).div(scaler.variance).col(0)

        nn.operations.forEach {
            inputArray = it(inputArray)
        }

        check(inputArray.size == 1) { "Neural network have several outputs" }
        return inputArray[0]
    }
}
