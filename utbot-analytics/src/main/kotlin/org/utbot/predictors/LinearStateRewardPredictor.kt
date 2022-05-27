package org.utbot.predictors

import org.utbot.analytics.UtBotAbstractPredictor
import org.utbot.framework.UtSettings
import smile.math.matrix.Matrix
import java.io.File

private const val DEFAULT_WEIGHT_PATH = "linear.txt"

/**
 * Last weight is bias
 */
private fun loadWeights(path: String): Matrix {
    val weightsFile = File("${UtSettings.rewardModelPath}/${path}")
    val weightsArray = weightsFile.readText().splitByCommaIntoDoubleArray()
    return Matrix(weightsArray)
}

class LinearStateRewardPredictor(weightsPath: String = DEFAULT_WEIGHT_PATH) :
    UtBotAbstractPredictor<List<List<Double>>, List<Double>> {
    private val weights = loadWeights(weightsPath)

    override fun predict(input: List<List<Double>>): List<Double> {
        // add 1 to each feature vector
        val matrixValues = input
            .map { (it + 1.0).toDoubleArray() }
            .toTypedArray()

        val X = Matrix(matrixValues)

        return X.mm(weights).col(0).toList()
    }
}