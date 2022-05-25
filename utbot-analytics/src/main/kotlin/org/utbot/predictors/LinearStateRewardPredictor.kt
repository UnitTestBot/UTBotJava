package org.utbot.predictors

import org.utbot.analytics.UtBotAbstractPredictor
import org.utbot.framework.UtSettings
import java.io.File
import smile.math.matrix.Matrix

/**
 * Last weight is bias
 */
private fun loadWeights(path: String): Matrix {
    return Matrix(File("${UtSettings.rewardModelPath}/${path}").readText().split(",").map(String::toDouble).toDoubleArray())
}

class LinearStateRewardPredictor : UtBotAbstractPredictor<List<List<Double>>, List<Double>> {
    val weights = loadWeights("linear.txt")

    override fun predict(input: List<List<Double>>): List<Double> {
        // add 1 to each feature vector
        val X = Matrix(input.map { it.toMutableList().also { featureVector ->
            featureVector.add(1.0)
        }.toDoubleArray() }.toTypedArray())
        return X.mm(weights).col(0).toList()
    }
}