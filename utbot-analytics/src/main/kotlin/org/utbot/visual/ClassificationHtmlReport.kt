package org.utbot.visual

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import tech.tablesaw.plotly.components.Figure

class ClassificationHtmlReport : AbstractHtmlReport() {

    private var figuresNum = 0

    fun addHeader(modelName: String, properties: Properties) {
        val currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"))
        builder.addRawHTML("<h1>Model : ${modelName}</h1>")
        builder.addRawHTML("<h2>$currentDateTime</h2>")
        builder.addRawHTML("<h3>Hyperparameters:</h3>")
        for (property in properties) {
            builder.addText("${property.key} : ${property.value}")
        }
    }

    private fun addFigure(figure: Figure) {
        builder.addRawHTML("<div id='plot$figuresNum'>")
        builder.addRawHTML(figure.asJavascript("plot$figuresNum"))
        builder.addRawHTML("</div>")
        figuresNum++
    }

    fun addDataDistribution(y: DoubleArray, threshold: Double = 1000.0) {
        val (data, notPresented) = y.partition { x -> x <= threshold }
        val rawHistogram = FigureBuilders.buildHistogram(
            data.toDoubleArray(),
            xLabel = "ms",
            yLabel = "Number of samples",
            title = "Raw data distribution"
        )
        addFigure(rawHistogram)
        builder.addText("Number of samples: ${y.size}")
        if (notPresented.isNotEmpty()) {
            builder.addText(
                "And ${notPresented.size} more samples longer than $threshold ms are not presented " +
                        "in the raw data distribution plot \n\n"
            )
        }
    }

    fun addConfusionMatrix(confusionMatrix: Array<DoubleArray>) {
        addFigure(
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

        addFigure(
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
        addFigure(
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
}