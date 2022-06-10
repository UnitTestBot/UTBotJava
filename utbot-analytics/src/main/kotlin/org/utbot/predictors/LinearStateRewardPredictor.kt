package org.utbot.predictors

import mu.KotlinLogging
import org.utbot.analytics.UtBotAbstractPredictor
import org.utbot.framework.PathSelectorType
import org.utbot.framework.UtSettings
import smile.math.matrix.Matrix
import java.io.File

private const val DEFAULT_WEIGHT_PATH = "linear.txt"

private val logger = KotlinLogging.logger {}

/**
 * Last weight is bias
 */
private fun loadWeights(path: String): Matrix {
    val weightsFile = File("${UtSettings.rewardModelPath}/${path}")

    if (!weightsFile.exists()) {
        error("There is no file with weights with path: ${weightsFile.absolutePath}")
    }

    val weightsArray = weightsFile.readText().splitByCommaIntoDoubleArray()

    return Matrix(weightsArray)
}

class LinearStateRewardPredictor(weightsPath: String = DEFAULT_WEIGHT_PATH) :
    UtBotAbstractPredictor<List<List<Double>>, List<Double>> {
    private lateinit var weights: Matrix

    init {
        try {
            weights = loadWeights(weightsPath)
        } catch (e: Exception) {
            logger.error(e) {
                "Error while initialization of LinearStateRewardPredictor. Changing pathSelectorType on INHERITORS_SELECTOR"
            }
            UtSettings.pathSelectorType = PathSelectorType.INHERITORS_SELECTOR
        }
    }

    override fun predict(input: List<List<Double>>): List<Double> {
        // add 1 to each feature vector
        val matrixValues = input
            .map { (it + 1.0).toDoubleArray() }
            .toTypedArray()

        val X = Matrix(matrixValues)

        return X.mm(weights).col(0).toList()
    }
}