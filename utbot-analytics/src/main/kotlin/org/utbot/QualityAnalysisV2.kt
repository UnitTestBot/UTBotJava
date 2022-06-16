package org.utbot

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.utbot.visual.FigureBuilders
import org.utbot.visual.HtmlBuilder
import java.io.File
import java.nio.file.Paths


fun parseReport(path: String, classes: Set<String>): Map<String, Double> {
    val result = mutableMapOf<String, Double>()
    val contestDocument: Document = Jsoup.parse(File("$path\\index.html"), null)
    val packageRows: Elements = contestDocument.select("table")[1].select("tr")
    for (i in 1 until packageRows.size) {
        val packageHref = packageRows[i].select("td")[0].select("a").attr("href")
        val packageDocument: Document = Jsoup.parse(File("$path\\$packageHref"), null)
        val clsRows: Elements = packageDocument.select("table")[1].select("tr")

        for (j in 1 until clsRows.size) {
            val clsName =
                clsRows[j].select("td")[0].select("a").attr("href").replace(".classes/", "").replace(".html", "")
            val fullName = packageHref.replace("/index.html", ".$clsName")

            if (classes.contains(fullName)) {
                result.put(fullName, clsRows[j].select("td")[3].select("span")[0].text().replace("%", "").toDouble())
            }
        }
    }

    return result
}


fun main() {
    val htmlBuilder = HtmlBuilder()

    val classes = mutableSetOf<String>()
    File(QualityAnalysisConfig.classesList).inputStream().bufferedReader().forEachLine { classes.add(it) }

    // Parse data
    val jacocoCoverage = QualityAnalysisConfig.selectors.map {
        it to parseReport("eval/jacoco/${QualityAnalysisConfig.project}/${it}", classes)
    }

    // Coverage report
    htmlBuilder.addHeader("Line coverage")
    jacocoCoverage.forEach {
        htmlBuilder.addText("Mean(Line (${it.first} model)) =" + it.second.map { it.value }.sum() / it.second.size)
    }
    htmlBuilder.addFigure(
        FigureBuilders.buildBoxPlot(
            jacocoCoverage.map { it.second.map { it2 -> "Line (${it.first} model)" } }.flatten().toTypedArray(),
            jacocoCoverage.map { it.second.map { it.value } }.flatten().toDoubleArray(),
            title = "Coverage",
            xLabel = "Instructions",
            yLabel = "Count"
        )
    )

    // Save report
    htmlBuilder.saveHTML(
        Paths.get(
            QualityAnalysisConfig.outputDir,
            QualityAnalysisConfig.project,
            QualityAnalysisConfig.selectors.joinToString("_")
        ).toFile().absolutePath
    )
}