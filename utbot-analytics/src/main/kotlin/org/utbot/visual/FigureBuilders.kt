package org.utbot.visual

import tech.tablesaw.plotly.components.Axis
import tech.tablesaw.plotly.components.Figure
import tech.tablesaw.plotly.components.Layout
import tech.tablesaw.plotly.components.Line
import tech.tablesaw.plotly.traces.*


class FigureBuilders {
    companion object {
        private fun getXYLayout(
                xLabel: String = "X",
                yLabel: String = "Y",
                title: String = "Plot"
        ): Layout {
            return Layout.builder()
                    .title(title)
                    .xAxis(Axis.builder().title(xLabel).build())
                    .yAxis(Axis.builder().title(yLabel).build())
                    .build()
        }

        private fun getXYZLayout(
                xLabel: String = "X",
                yLabel: String = "Y",
                zLabel: String = "Y",
                title: String = "Plot"
        ): Layout {
            return Layout.builder()
                    .title(title)
                    .xAxis(Axis.builder().title(xLabel).build())
                    .yAxis(Axis.builder().title(yLabel).build())
                    .zAxis(Axis.builder().title(zLabel).build())
                    .build()
        }

        fun buildScatterPlot(
                x: DoubleArray,
                y: DoubleArray,
                xLabel: String = "X",
                yLabel: String = "Y",
                title: String = "Scatter plot"
        ): Figure {
            val layout = getXYLayout(xLabel, yLabel, title)
            val trace: Trace = ScatterTrace.builder(x, y).build()

            return Figure(layout, trace)
        }

        fun buildHistogram(
                data: DoubleArray,
                xLabel: String = "X",
                yLabel: String = "Y",
                title: String = "Histogram"
        ): Figure {
            val layout = getXYLayout(xLabel, yLabel, title)
            val trace: Trace = HistogramTrace.builder(data).build()

            return Figure(layout, trace)
        }

        fun build2DHistogram(
                x: DoubleArray,
                y: DoubleArray,
                xLabel: String = "X",
                yLabel: String = "Y",
                title: String = "Histogram 2D"
        ): Figure {
            val layout = getXYLayout(xLabel, yLabel, title)
            val trace: Trace = Histogram2DTrace.builder(x, y).build()

            return Figure(layout, trace)
        }

        fun buildBarPlot(
                x: Array<Any>,
                y: DoubleArray,
                xLabel: String = "X",
                yLabel: String = "Y",
                title: String = "BarPlot"
        ): Figure {
            val layout = getXYLayout(xLabel, yLabel, title)
            val trace: Trace = BarTrace.builder(x, y).build()
            return Figure(layout, trace)
        }

        fun buildHeatmap(
                x: Array<Any>,
                y: Array<Any>,
                z: Array<DoubleArray>,
                xLabel: String = "X",
                yLabel: String = "Y",
                title: String = "Heatmap"
        ): Figure {
            val layout = getXYLayout(xLabel, yLabel, title)
            val trace: Trace = HeatmapTrace.builder(x, y, z).build()
            return Figure(layout, trace)
        }

        fun buildLinePlot(
                x: DoubleArray,
                y: DoubleArray,
                xLabel: String = "X",
                yLabel: String = "Y",
                title: String = "Line plot"
        ): Figure {
            val layout = getXYLayout(xLabel, yLabel, title)
            val trace: Trace = ScatterTrace.builder(x, y).mode(ScatterTrace.Mode.LINE_AND_MARKERS).build()

            return Figure(layout, trace)
        }

        fun buildTwoLinesPlot(
                y1: DoubleArray,
                y2: DoubleArray,
                xLabel: String = "X",
                yLabel: String = "Y",
                title: String = "Two lines plot"
        ): Figure {
            val layout = getXYLayout(xLabel, yLabel, title)
            val trace1: Trace = ScatterTrace.builder(DoubleArray(y1.size) { it.toDouble() }, y1).mode(ScatterTrace.Mode.LINE_AND_MARKERS).build()
            val trace2: Trace = ScatterTrace.builder(DoubleArray(y2.size) { it.toDouble() }, y2).mode(ScatterTrace.Mode.LINE_AND_MARKERS).build()

            return Figure(layout, trace1, trace2)
        }

        fun buildSeveralLinesPlot(
            x: List<DoubleArray>,
            y: List<DoubleArray>,
            colors: List<String>,
            names: List<String>,
            xLabel: String = "X",
            yLabel: String = "Y",
            title: String = "Line plot"
        ): Figure {
            val layout = getXYLayout(xLabel, yLabel, title)
            val traces = x.indices.map {
                ScatterTrace.builder(x[it], y[it])
                    .mode(ScatterTrace.Mode.LINE_AND_MARKERS)
                    .line(Line.builder().shape(Line.Shape.LINEAR).color(colors[it]).build())
                    .name(names[it])
                    .showLegend(true)
                    .build()
            }

            return Figure(layout, *traces.toTypedArray())
        }

        fun buildBoxPlot(x: Array<String>,
                         y: DoubleArray,
                         xLabel: String = "X",
                         yLabel: String = "Y",
                         title: String = "Box plot"): Figure {
            val layout = getXYLayout(xLabel, yLabel, title)
            val trace: BoxTrace = BoxTrace.builder(x, y).build()
            return Figure(layout, trace)
        }

    }

}