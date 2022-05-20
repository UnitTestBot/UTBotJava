package org.utbot.models

import org.utbot.TrainRegressionModel
import smile.base.cart.Loss
import smile.data.DataFrame
import smile.data.formula.Formula
import smile.regression.*


class SimpleRegression {

    var gradientBoostModel: DataFrameRegression? = null
    var linearRegressionModel: DataFrameRegression? = null
    var lassoModel: DataFrameRegression? = null
    var cartModel: DataFrameRegression? = null
    var randomForestModel: DataFrameRegression? = null

    // train with default params
    fun trainAllModels(formula: Formula, data: DataFrame) {
        trainLassoModel(formula, data)
        trainLinearRegression(formula, data)
        trainGradientTreeBoosting(formula, data)
        trainCART(formula, data)
        trainRandomForest(formula, data)
    }

    //prediction by all models
    fun predictionByAllModels(formula: Formula, data: DataFrame): Map<String, Pair<DoubleArray, DoubleArray?>> {
        val actualLabel: DoubleArray = formula.y(data).toDoubleArray()

        val gbmPrediction = gradientBoostModel?.predict(data)
        val lrmPrediction = linearRegressionModel?.predict(data)
        val lassoPrediction = lassoModel?.predict(data)
        val cartPrediction = cartModel?.predict(data)
        val randomForestPrediction = randomForestModel?.predict(data)

        return mapOf(
                "gbm" to Pair(actualLabel, gbmPrediction),
                "lrm" to Pair(actualLabel, lrmPrediction),
                "lasso" to Pair(actualLabel, lassoPrediction),
                "cart" to Pair(actualLabel, cartPrediction),
                "randomForest" to Pair(actualLabel, randomForestPrediction)
        )
    }

    // loss - loss function for regression.
    // ntrees - the number of iterations (trees).
    // maxDepth - the maximum depth of the tree.
    // maxNodes - the maximum number of leaf nodes in the tree.
    // nodeSize - the minimum size of leaf nodes.
    // shrinkage - the shrinkage parameter in (0, 1] controls the learning rate of procedure.
    // subsample - the sampling fraction for stochastic tree boosting.
    fun trainGradientTreeBoosting(
            formula: Formula, data: DataFrame,
            loss: Loss = Loss.lad(),
            ntrees: Int = 25, maxDepth: Int = 20,
            maxNodes: Int = 6, nodeSize: Int = 5,
            shrinkage: Double = 0.05, subsample: Double = 0.7
    ) {
        gradientBoostModel = gbm(formula, data, loss, ntrees, maxDepth, maxNodes, nodeSize, shrinkage, subsample)
    }

    // method - the fitting method ("svd" or "qr").
    // recursive - if true, the return model supports recursive least squares.
    fun trainLinearRegression(
            formula: Formula, data: DataFrame,
            method: String = "qr", stderr: Boolean = true,
            recursive: Boolean = true
    ) {
        linearRegressionModel = lm(formula, data, method, stderr, recursive)
    }

    fun trainLassoModel(formula: Formula, data: DataFrame) {
        lassoModel = lasso(formula, data, 100.0)
    }

    /*
    * maxDepth - the maximum depth of the tree.
    * maxNodes - the maximum number of leaf nodes in the tree.
    * nodeSize - the minimum size of leaf nodes
    * */
    fun trainCART(formula: Formula, data: DataFrame, maxDepth: Int = 20, maxNodes: Int = 50, nodeSize: Int = 5) {
        cartModel = cart(formula, data, maxDepth, maxNodes, nodeSize)
    }

    /*
    * ntrees - the number of trees.
    * mtry - the number of input variables to be used to determine the decision at a node of the tree.
    *       dim/3 seems to give generally good performance, where dim is the number of variables.
    * maxDepth - the maximum depth of the tree.
    * maxNodes - the maximum number of leaf nodes in the tree.
    * nodeSize - the minimum size of leaf nodes.
    * subsample - the sampling rate for training tree. 1.0 means sampling with replacement. < 1.0 means sampling without replacement.
    * */
    fun trainRandomForest(
            formula: Formula, data: DataFrame, ntrees: Int = 25, mtry: Int = 5, maxDepth: Int = 20,
            maxNodes: Int = 50, nodeSize: Int = 5, subsample: Double = 1.0
    ) {
        randomForestModel = randomForest(
                formula, data, ntrees, mtry, maxDepth,
                maxNodes, nodeSize, subsample
        )
    }

    fun prediction(
            formula: Formula,
            data: DataFrame,
            trainRegressionModel: TrainRegressionModel
    ): Map<String, Pair<DoubleArray, DoubleArray?>> {
        val actualLabel: DoubleArray = formula.y(data).toDoubleArray()
        return when (trainRegressionModel) {
            TrainRegressionModel.GBM -> {
                mapOf(
                        "gbm" to Pair(actualLabel, gradientBoostModel?.predict(data))
                )
            }
            TrainRegressionModel.LRM -> {
                mapOf(
                        "lrm" to Pair(actualLabel, linearRegressionModel?.predict(data))
                )
            }
            TrainRegressionModel.LASSO -> {
                mapOf(
                        "lasso" to Pair(actualLabel, lassoModel?.predict(data))
                )
            }
            TrainRegressionModel.CART -> {
                mapOf(
                        "cart" to Pair(actualLabel, cartModel?.predict(data))
                )
            }
            TrainRegressionModel.RANDOMFOREST -> {
                mapOf(
                        "random forest" to Pair(actualLabel, randomForestModel?.predict(data))
                )
            }
            else -> predictionByAllModels(formula, data)
        }
    }
}