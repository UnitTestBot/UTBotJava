package org.utbot.predictors

import org.utbot.analytics.IncrementalData
import org.utbot.analytics.UtBotAbstractPredictor
import org.utbot.analytics.UtBotNanoTimePredictor
import org.utbot.engine.pc.UtExpression
import org.utbot.features.Transform
import org.utbot.features.UtExpressionStructureCounter
import org.utbot.features.featureIndex
import org.utbot.models.loadModel
import smile.classification.Classifier
import java.io.File
import java.io.FileOutputStream


@Suppress("unused")
class UtBotTimePredictor : UtBotAbstractPredictor<Iterable<UtExpression>, Long>,
        UtBotNanoTimePredictor<Iterable<UtExpression>> {

    private val saveFile = "logs/stats.txt"

    private val transform: Transform = loadModel("transform")
    private val model: Classifier<DoubleArray> = loadModel("predictor")

    init {
        File(saveFile).printWriter().use { out ->
            out.println("EXEC_TIME," + featureIndex.keys.joinToString(separator = ","))
        }
    }

    override fun predict(input: Iterable<UtExpression>): Long {
        val extract = UtExpressionStructureCounter(input).extract()

        val features = transform.apply(extract)

        return model.predict(features).toLong()
    }

    override fun provide(input: Iterable<UtExpression>, expectedResult: Long, actualResult: Long) {
        val extract = UtExpressionStructureCounter(input).extract()

        FileOutputStream(saveFile, true).bufferedWriter().use { out ->
            out.appendLine((actualResult.toDouble() / 1_000_000).toString() + "," +
                    extract.joinToString(separator = ","))
        }
    }
}

class UtBotTimePredictorIncremental : UtBotAbstractPredictor<IncrementalData, Long>,
        UtBotNanoTimePredictor<IncrementalData> {

    private val saveFile = "logs/stats.txt"
    private val transform: Transform = loadModel("transform")
    private val model: Classifier<DoubleArray> = loadModel("predictor")

    init {
        File(saveFile).printWriter().use { out ->
            out.println("EXEC_TIME," + featureIndex.keys.joinToString(separator = ",") { "All$it" } + "," +
                    featureIndex.keys.joinToString(separator = ",") { "New$it" }
            )
        }
    }

    override fun provide(input: IncrementalData, expectedResult: Long, actualResult: Long) {
        val allFeatures = UtExpressionStructureCounter(input.constraints).extract()
        val newFeatures = UtExpressionStructureCounter(input.constraintsToAdd).extract()

        FileOutputStream(saveFile, true).bufferedWriter().use { out ->
            out.write((actualResult.toDouble() / 1_000_000).toString() + "," +
                    allFeatures.joinToString(separator = ",") + "," +
                    newFeatures.joinToString(separator = ","));
            out.newLine()
        }
    }

    override fun predict(input: IncrementalData): Long {
        val allFeatures = UtExpressionStructureCounter(input.constraints).extract()
        val newFeatures = UtExpressionStructureCounter(input.constraintsToAdd).extract()

        val features = transform.apply(allFeatures + newFeatures)

        return model.predict(features).toLong()
    }
}