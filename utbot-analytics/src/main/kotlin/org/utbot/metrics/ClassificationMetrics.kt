package org.utbot.metrics

import org.utbot.features.Transform
import smile.classification.Classifier
import smile.validation.metric.ConfusionMatrix
import kotlin.math.round

@Suppress("ArrayInDataClass")
data class ClassificationMetrics(
        val modelName: String,
        var numCls: Int = 0,
        var f1Macro: Double = 0.0,
        var acc: Double = 0.0,
        var precision: Array<Double> = Array(numCls) { 0.0 },
        var recall: Array<Double> = Array(numCls) { 0.0 },
        var avgPredTime: Double = 0.0
) {

    lateinit var confusionMatrix: ConfusionMatrix

    constructor(modelName: String, model: Classifier<DoubleArray>, transform: Transform, target: IntArray, input: Array<DoubleArray>) : this(modelName) {
        assert(input.size == target.size)
        avgPredTime = 0.0

        val prediction = input.map {
            val startTime = System.nanoTime()
            val pred = model.predict(transform.apply(it))
            avgPredTime += (System.nanoTime() - startTime).toDouble() / 1_000_000
            pred
        }
        avgPredTime /= target.size
        confusionMatrix = ConfusionMatrix.of(target, prediction.toIntArray())

        val diag = (confusionMatrix.matrix.indices).map { confusionMatrix.matrix[it][it].toDouble() }
        numCls = diag.size
        precision = Array(numCls) { 0.0 }
        recall = Array(numCls) { 0.0 }
        for (i in confusionMatrix.matrix.indices) {
            precision[i] = diag[i] / confusionMatrix.matrix[i].sum()
            recall[i] = diag[i] / confusionMatrix.matrix.sumBy { row -> row[i] }
        }

        acc = diag.sum() / confusionMatrix.matrix.sumBy { it.sum() }
        f1Macro = (confusionMatrix.matrix.indices).sumByDouble {
            2 * precision[it] * recall[it] / (precision[it] + recall[it])
        } / numCls
    }

    override fun toString(): String {
        return "modelName:$modelName" +
                "\nnumCls:$numCls" +
                "\nf1Macro:${round(f1Macro * 100) / 100}" +
                "\nacc:${round(acc * 100) / 100}" +
                "\nprecision:[${precision.joinToString(" ") { (round(it * 100) / 100).toString() }}]" +
                "\nrecall:[${recall.joinToString(" ") { (round(it * 100) / 100).toString() }}]" +
                "\navgPredTime:${avgPredTime}"
    }

    fun getNormalizedConfusionMatrix(): Array<DoubleArray> {
        return confusionMatrix.matrix
                .mapIndexed { index, counts ->
                    counts.map { it.toDouble() / confusionMatrix.matrix[index].sum() }
                            .toDoubleArray()
                }.toTypedArray()
    }

    operator fun compareTo(metrics: ClassificationMetrics) = this.acc.compareTo(metrics.acc)
}