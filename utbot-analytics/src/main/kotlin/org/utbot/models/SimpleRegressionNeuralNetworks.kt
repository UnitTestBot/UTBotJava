package org.utbot.models

import org.utbot.TrainRegressionModel
import smile.base.mlp.Layer
import smile.data.DataFrame
import smile.data.formula.Formula
import smile.math.MathEx
import smile.math.TimeFunction
import smile.regression.MLP
import smile.regression.RBFNetwork
import smile.regression.rbfnet


class SimpleRegressionNeuralNetworks {
    var rbfModel: RBFNetwork<DoubleArray>? = null
    var mlp: MLP? = null

    fun trainByAllModels(formula: Formula, data: DataFrame) {
        trainRBFModel(formula, data)
        trainMLP(formula, data)
    }

    fun predictionByAllModels(formula: Formula, data: DataFrame): Map<String, Pair<DoubleArray, DoubleArray?>> {
        val actualLabel: DoubleArray = formula.y(data).toDoubleArray()
        val features = formula.x(data).toArray()

        val rbfPrediction = rbfModel?.predict(features)
        val mlpPrediction = mlp?.predict(features)
        return mapOf(
                "rbf" to Pair(actualLabel, rbfPrediction),
                "mlp" to Pair(actualLabel, mlpPrediction)
        )
    }

    // k - number of neurons
    fun trainRBFModel(formula: Formula, data: DataFrame, k: Int = 10, normalized: Boolean = false) {
        val features = formula.x(data).toArray()
        val labels = formula.y(data).toDoubleArray()
        rbfModel = rbfnet(features, labels, k, normalized)
    }


    fun trainMLP(
            formula: Formula,
            data: DataFrame,
            hidden: Int = 16,
            epochs: Int = 200,
            lr: Double = 0.001,
            lambda: Double = 0.001
    ) {
        val numInput = data.schema().fields().size - 1
        val features = formula.x(data).toArray()
        val labels = formula.y(data).toDoubleArray()

        mlp = MLP(numInput, Layer.rectifier(hidden), Layer.sigmoid(hidden / 2))
                .also {
                    it.setLearningRate(TimeFunction.constant(lr))
                    it.weightDecay = lambda
                }

        mlp?.apply {
            for (epoch in 1..epochs) {
                val permutation: IntArray = MathEx.permutate(features.size)
                for (i in permutation) {
                    this.update(features[i], labels[i])
                }
            }
        }
    }

    fun prediction(
            formula: Formula,
            data: DataFrame,
            trainRegressionModel: TrainRegressionModel
    ): Map<String, Pair<DoubleArray, DoubleArray?>> {
        val actualLabel: DoubleArray = formula.y(data).toDoubleArray()
        val features = formula.x(data).toArray()
        return when (trainRegressionModel) {
            TrainRegressionModel.RBF -> mapOf(
                    "rbf" to Pair(actualLabel, rbfModel?.predict(features))
            )
            TrainRegressionModel.MLP -> mapOf(
                    "mlp" to Pair(actualLabel, mlp?.predict(features))
            )
            else -> predictionByAllModels(formula, data)
        }
    }
}