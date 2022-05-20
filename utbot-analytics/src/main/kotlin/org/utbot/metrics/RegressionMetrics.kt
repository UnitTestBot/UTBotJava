package org.utbot.metrics

import kotlin.math.pow
import kotlin.math.sqrt

data class RegressionMetrics(
        val modelName: String,
        var mae: Double = 0.0,
        var mse: Double = 0.0,
        var rmse: Double = 0.0
) {

    private val eps: Double = 0.01
    private val absoluteErrors = mutableMapOf<Int, Double>()
    private val absolutePercentageErrors = mutableMapOf<Int, Double>()

    constructor(modelName: String, prediction: DoubleArray, target: DoubleArray) : this(modelName) {
        assert(prediction.size == target.size)
        for (i in prediction.indices) {
            val variance = kotlin.math.abs(prediction[i] - target[i])
            val powVariance = variance.pow(2)

            mae += variance
            mse += powVariance
            rmse += powVariance

            absoluteErrors[i] = variance
            absolutePercentageErrors[i] = variance / (target[i] + eps) * 100
        }

        mae /= prediction.size
        mse /= prediction.size
        rmse = sqrt(rmse / prediction.size)
    }

    override fun toString(): String {
        return "$modelName mae:$mae rmse:$rmse"
    }
}