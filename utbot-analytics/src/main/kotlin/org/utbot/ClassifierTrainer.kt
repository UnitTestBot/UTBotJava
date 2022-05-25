package org.utbot

import org.utbot.features.Compose
import org.utbot.features.featureIndexHash
import org.utbot.metrics.ClassificationMetrics
import org.utbot.models.ClassifierModel
import org.utbot.models.loadModelFromJson
import org.utbot.models.save
import org.utbot.visual.ClassificationHTMLReport
import smile.classification.Classifier
import smile.data.CategoricalEncoder
import smile.data.DataFrame
import smile.data.formula.Formula
import smile.data.vector.IntVector
import smile.read
import java.io.File
import java.util.*

private const val dataPath = "logs/stats.txt"
private const val logDir = "logs"

class ClassifierTrainer(data: DataFrame, val classifierModel: ClassifierModel = ClassifierModel.GBM) :
        AbstractTrainer(data, savePcaVariance = true) {
    private lateinit var metrics: ClassificationMetrics
    lateinit var model: Classifier<DoubleArray>
    val properties = Properties()
    private var classSizesAfterResampling: DoubleArray
    private var classSizesBeforeResampling: DoubleArray

    init {
        val indicesBeforeResampling = resample.groupIndicesByLabel(data)
        classSizesBeforeResampling = indicesBeforeResampling.map { it.size.toDouble() }.toDoubleArray()

        val indicesAfterResampling = resample.groupIndicesByLabel(resample.transform(data))
        classSizesAfterResampling = indicesAfterResampling.map { it.size.toDouble() }.toDoubleArray()

        trainData = remap(trainData)
        validationData = remap(validationData)
        properties.putAll(loadModelFromJson("models.json", classifierModel.name))
    }

    private fun remap(remapData: DataFrame): DataFrame {
        val indices = resample.groupIndicesByLabel(remapData)

        val labels = IntArray(remapData.nrows()) { 0 }
        for ((label, array) in indices.withIndex()) {
            for (i in array) labels[i] = label
        }
        val xFrame = Formula.lhs(targetColumn).x(remapData)
        val x = xFrame.toArray(false, CategoricalEncoder.LEVEL)
        val nTrainData = DataFrame.of(x, *xFrame.names())

        return nTrainData.merge(IntVector.of(targetColumn, labels))
    }

    override fun train() {
        model = classifierModel.train(Formula.lhs(targetColumn), trainData, properties)
    }

    override fun validate() {
        val actualLabel: DoubleArray = Formula.lhs(targetColumn).y(validationData).toDoubleArray()
        val xFrame = Formula.lhs(targetColumn).x(validationData)
        val x = xFrame.toArray(false, CategoricalEncoder.LEVEL)

        metrics = ClassificationMetrics(classifierModel.name, model, Compose(transforms), actualLabel.map { it.toInt() }.toIntArray(), x)
    }

    override fun visualize() {
        val report = ClassificationHTMLReport()
        report.run {
            addHeader(classifierModel.name, properties)
            addDataDistribution(formula.y(data).toDoubleArray())
            addClassDistribution(classSizesBeforeResampling)
            addClassDistribution(classSizesAfterResampling, before = false)
            addPCAPlot(pcaVarianceProportion, pcaCumulativeVarianceProportion)
            addMetrics(metrics.acc, metrics.f1Macro, metrics.avgPredTime, metrics.precision.toDoubleArray(),
                    metrics.recall.toDoubleArray())
            addConfusionMatrix(metrics.getNormalizedConfusionMatrix())
            save()
        }
    }

    override fun featureSelection() {
        // Uncomment for feature selection
//        transforms.add(FeatureSelection(trainData, validationData, Formula.lhs(targetColumn), classifierModel, properties))
//        trainData = transforms.last().apply(trainData)
    }

    override fun save() {
        save(Compose(transforms), "$logDir/transform")
    }
}


fun main() {
    val data = read.csv(dataPath)
    val model = ClassifierModel.GBM
    val trainer = ClassifierTrainer(data, classifierModel = model)
    trainer.fit()

    save(trainer.model, "$logDir/predictor")
    File("$logDir/META-INF.txt").printWriter().use { out ->
        out.println("FeatureExtraction=UtExpressionStructureCounter")
        out.println("FeatureExtractionHash=$featureIndexHash")
        out.println("ModelType=" + model.name)
        out.println("Transforms=" + Compose(trainer.transforms).toString())
    }
}