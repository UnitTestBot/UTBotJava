package org.utbot.visual

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class ClassificationHTMLReport {
    private val builder = HtmlBuilder()

    fun addHeader(modelName: String, properties: Properties) {
        builder.addHeader(modelName, properties)
    }

    fun addDataDistribution(y: DoubleArray, threshold: Double = 1000.0) {
        val rawHistogram = FigureBuilders.buildHistogram(
            y.filter { x -> x <= threshold }.toDoubleArray(),
            xLabel = "ms",
            yLabel = "Number of samples",
            title = "Raw data distribution"
        )
        builder.addFigure(rawHistogram)
        builder.addText("Number of samples: ${y.size}")
        builder.addText(
            "And ${
                y.filter { x -> x > threshold }.size
            } more samples longer than $threshold ms are not presented in the raw data distribution plot \n\n"
        )
    }

    fun addConfusionMatrix(confusionMatrix: Array<DoubleArray>) {
        builder.addFigure(
            FigureBuilders.buildHeatmap(
                Array(confusionMatrix.size) { it },
                Array(confusionMatrix.size) { it },
                confusionMatrix,
                xLabel = "Predicted class",
                yLabel = "Reference class",
                title = "Confusion matrix"
            )
        )
    }

    fun addClassDistribution(classSizes: DoubleArray, before: Boolean = true) {
        var title = "Class distribution after resampling"
        if (before) title = "Class distribution before resampling"

        builder.addFigure(
            FigureBuilders.buildBarPlot(
                Array(classSizes.size) { it },
                classSizes,
                title = title,
                xLabel = "Classes",
                yLabel = "Number of samples"
            )
        )
    }

    fun addPCAPlot(variance: DoubleArray, cumulativeVariance: DoubleArray) {
        builder.addFigure(
            FigureBuilders.buildTwoLinesPlot(
                variance,
                cumulativeVariance,
                title = "PCA variance and cumulative variance",
                xLabel = "PC",
                yLabel = "Variance"
            )
        )
    }

    fun addMetrics(acc: Double, f1: Double, predTime: Double, precision: DoubleArray, recall: DoubleArray) {
        builder.addText("Accuracy ${String.format("%.3f", acc)}")
        builder.addText("F1 macro ${String.format("%.3f", f1)}")
        builder.addText("AvgPredTime ${String.format("%.3f", predTime)}(ms)")
        for ((i, value) in precision.withIndex()) {
            builder.addText(
                "For class $i precision ${String.format("%.3f", value)} recall ${
                    String.format(
                        "%.3f",
                        recall[i]
                    )
                }"
            )
        }
    }

    fun save(filename: String = "default") {
        if (filename == "default") {
            val filename = "logs/Classification_Report_" +
                    LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss")) +
                    ".html"
            builder.saveHTML(filename)
        } else {
            builder.saveHTML(filename)
        }
    }

}