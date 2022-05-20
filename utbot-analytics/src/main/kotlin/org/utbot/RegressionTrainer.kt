package org.utbot

import org.utbot.TrainRegressionModel.*
import org.utbot.metrics.RegressionMetrics
import org.utbot.models.SimpleRegression
import org.utbot.models.SimpleRegressionNeuralNetworks
import smile.data.DataFrame
import smile.data.formula.Formula
import smile.read

class RegressionTrainer(data: DataFrame, val trainRegressionModel: TrainRegressionModel = GBM) :
        AbstractTrainer(data) {
    val simpleRegression = SimpleRegression()
    val simpleNeuralNetworks = SimpleRegressionNeuralNetworks()
    var validationResult: Map<String, Pair<DoubleArray, DoubleArray?>>? = null
    var metrics: Map<String, RegressionMetrics>? = null

    override fun train() {
        when (trainRegressionModel) {
            CART -> simpleRegression.trainCART(Formula.lhs(targetColumn), trainData)
            GBM -> simpleRegression.trainGradientTreeBoosting(Formula.lhs(targetColumn), trainData)
            LRM -> simpleRegression.trainLinearRegression(Formula.lhs(targetColumn), trainData)
            LASSO -> simpleRegression.trainLassoModel(Formula.lhs(targetColumn), trainData)
            RANDOMFOREST -> simpleRegression.trainRandomForest(
                    Formula.lhs(targetColumn),
                    trainData
            )
            RBF -> simpleNeuralNetworks.trainRBFModel(Formula.lhs(targetColumn), trainData)
            MLP -> simpleNeuralNetworks.trainMLP(Formula.lhs(targetColumn), trainData)
        }
    }

    override fun validate() {
        validationResult = when (trainRegressionModel) {
            RBF, MLP -> simpleNeuralNetworks.prediction(
                    Formula.lhs(targetColumn).expand(validationData.schema()), validationData, trainRegressionModel
            )
            else -> simpleRegression.prediction(
                    Formula.lhs(targetColumn).expand(validationData.schema()), validationData, trainRegressionModel
            )
        }
        metrics = validationResult?.mapValues {
            RegressionMetrics(it.key, it.value.first, it.value.second!!)
        }
    }

    override fun visualize() {
        TODO("Not yet implemented")
    }

    override fun featureSelection() {
        TODO("Not yet implemented")
    }

    override fun save() {
        TODO("Not yet implemented")
    }
}

enum class TrainRegressionModel {
    GBM, LRM, LASSO, CART, RANDOMFOREST, RBF, MLP
}


fun main() {
    val data = read.csv("logs/stats.txt")

    values().forEach {
        val trainer = RegressionTrainer(data, trainRegressionModel = it)
        trainer.fit()
        trainer.metrics?.values?.forEach { println(it) }
    }
}