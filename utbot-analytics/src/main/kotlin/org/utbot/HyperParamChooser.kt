package org.utbot

import org.utbot.features.Compose
import org.utbot.models.ClassifierModel
import org.utbot.models.saveToJson
import smile.data.CategoricalEncoder
import smile.data.DataFrame
import smile.data.formula.Formula
import smile.read
import smile.validation.Hyperparameters
import smile.validation.metric.Accuracy
import java.util.*

private val dataPath = "logs/stats.txt"

data class SearchResult(val score: Double, val properties: Properties)

class HyperParamChooser {

    /**
     * Note: limitation < 0 - unlimited combination of properties
     */
    fun search(
            trainingModel: ClassifierModel,
            formula: Formula,
            trainData: DataFrame,
            validationData: DataFrame,
            random: Boolean = false,
            limitation: Long = 20L,
            takeResult: Int = 50
    ): List<SearchResult> {
        val actualValidationLabel: IntArray = formula.y(validationData).toIntArray()
        val xFrame = formula.x(validationData)
        val x = xFrame.toArray(false, CategoricalEncoder.LEVEL)
        val result = mutableListOf<SearchResult>()
        val hyperParams = trainingModel.getHParams()

        val block: (prop: Properties) -> Unit = {
            val model = trainingModel.train(formula, trainData, it)
            val predictedValidationLabel = model.predict(x)
            val accuracy = Accuracy.of(actualValidationLabel, predictedValidationLabel)
            result.add(SearchResult(accuracy, it))
        }

        if (!random) gridSearchClassifier(
                block,
                hyperParams,
                limitation
        )
        else randSearchClassifier(
                block,
                hyperParams,
                limitation
        )
        return result.sortedByDescending { it.score }.take(takeResult)
    }

    private fun gridSearchClassifier(
            block: (prop: Properties) -> Unit,
            hyperParams: Hyperparameters,
            limitation: Long = 20L
    ) {
        if (limitation < 0) hyperParams.grid().forEach(block)
        else hyperParams.grid().limit(limitation).forEach(block)
    }

    private fun randSearchClassifier(
            block: (prop: Properties) -> Unit,
            hyperParams: Hyperparameters,
            limitation: Long = 20L
    ) {
        if (limitation < 0) hyperParams.random().forEach(block)
        else hyperParams.random().limit(limitation).forEach(block)
    }
}


fun main() {
    val classifierModel = ClassifierModel.GBM
    val data = read.csv(dataPath)
    val hpChooser = HyperParamChooser()

    val trainer = ClassifierTrainer(data, classifierModel = classifierModel)
    trainer.preprocess()
    trainer.validationData = Compose(trainer.transforms).apply(trainer.validationData)

    val props = hpChooser.search(classifierModel, Formula.lhs(trainer.targetColumn),
            trainer.trainData, trainer.validationData, random = true, limitation = 1)

    props.last().properties.saveToJson(Thread.currentThread().contextClassLoader.getResource("models.json")!!.file,
            classifierModel.name)
    props.forEach {
        println(it.score)
        println(it.properties)
    }
}