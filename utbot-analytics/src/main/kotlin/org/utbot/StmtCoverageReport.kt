package org.utbot

import org.utbot.visual.FigureBuilders
import org.utbot.visual.HtmlBuilder
import java.io.File
import java.nio.file.Paths
import kotlin.random.Random
import org.apache.commons.io.FileUtils
import smile.read


data class Statistics(
    val perMethod: Map<String, List<DoubleArray>>,
    val perClass: Map<String,  List<DoubleArray>>,
    val perProject:  List<DoubleArray>
)


fun getStatistics(path: String, classes: Set<String>): Statistics {
    var projectTotalStmts = 0.0
    var projectMinStartTime = Double.MAX_VALUE

    val rawStatistics = File(path).listFiles()?.filter { it.extension == "txt" && it.readLines().size > 1 }?.map {
            val data = read.csv(it.absolutePath)
            it.nameWithoutExtension to data.toArray()
        }?.toMap() ?: emptyMap()

    val statisticsPerMethod = rawStatistics.map { methodData ->
        val startTime = methodData.value[0][0]
        val value = methodData.value.map {
            doubleArrayOf(it[0] - startTime, it[1] / it[2]) // time, percent
        }.sortedBy { it[0] }

        methodData.key to value
    }.toMap()

    val statisticsPerClass = classes.map { cls ->
        var minStartTime = Double.MAX_VALUE
        var totalStmts = 0.0
        val filteredStatistics = rawStatistics.filter { it.key.contains(cls) }

        filteredStatistics.forEach {
            minStartTime = minOf(minStartTime, it.value[0][0])
            totalStmts += it.value.getOrNull(1)?.get(2) ?: 0.0
        }
        projectTotalStmts += totalStmts
        projectMinStartTime = minOf(minStartTime, projectMinStartTime)

        var prevCov = 0.0
        cls to filteredStatistics.toList().sortedBy { it.second[0][0] }.flatMap { methodData ->
            val value = methodData.second.map {
                doubleArrayOf(it[0] - minStartTime, (it[1] + prevCov) / totalStmts)
            }

            prevCov += methodData.second.last()[1]
            value
        }.sortedBy { it[0] }
    }.toMap()

    var prevCov = 0.0
    val statisticsPerProject = rawStatistics
        .filter { classes.any { it2 -> it.key.contains(it2) } }
        .toList()
        .sortedBy { it.second[0][0] }
        .flatMap { methodData ->
            val value = methodData.second.map {
                doubleArrayOf(it[0] - projectMinStartTime, (it[1] + prevCov) / projectTotalStmts)  // time, percent
            }

            prevCov += methodData.second.last()[1]
            value
        }.sortedBy { it[0] }

    return Statistics(statisticsPerMethod, statisticsPerClass, statisticsPerProject)
}

fun main() {
    // Processed classes
    val classes = mutableSetOf<String>()
    File(QualityAnalysisConfig.classesList).inputStream().bufferedReader().forEachLine { classes.add(it) }

    // Prepare folder
    val projectDataPath = "${QualityAnalysisConfig.outputDir}/report/${QualityAnalysisConfig.project}/${QualityAnalysisConfig.selectors.joinToString("_")}"
    File(projectDataPath).deleteRecursively()
    classes.forEach { File(projectDataPath, it).mkdirs() }
    File(projectDataPath, "css").mkdirs()
    listOf("css/coverage.css", "css/highlight-idea.css").forEach {
        FileUtils.copyInputStreamToFile(
            Statistics::class.java.classLoader.getResourceAsStream(it),
            Paths.get(projectDataPath, it).toFile()
        )
    }

    // Parse statistics
    val selectors = QualityAnalysisConfig.selectors
    val statistics = QualityAnalysisConfig.covStatistics.map { getStatistics(it, classes) }
    val colors = QualityAnalysisConfig.selectors.map {
        val rnd = Random.Default
        "rgb(${rnd.nextInt(256)}, ${rnd.nextInt(256)}, ${rnd.nextInt(256)})"
    }

    val htmlBuilderForProject = HtmlBuilder(pathToStyle = listOf("css/coverage.css", "css/highlight-idea.css"),
                                            pathToJs = listOf("https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.5.1/highlight.min.js"))
    htmlBuilderForProject.addTable(
        statistics.map { it.perClass.map { it.key to (it.value.lastOrNull()?.lastOrNull() ?: 0.0) * 100 }.toMap() },
        selectors,
        colors,
        "class"
    )
    htmlBuilderForProject.addFigure(
        FigureBuilders.buildSeveralLinesPlot(
            statistics.map { it.perProject.map { it[0] / 1e9 }.toDoubleArray() },
            statistics.map { it.perProject.map { it[1] * 100 }.toDoubleArray() },
            colors,
            selectors,
            xLabel = "Time (sec)",
            yLabel = "Coverage (%)",
            title = QualityAnalysisConfig.project
        )
    )
    htmlBuilderForProject.saveHTML(File(projectDataPath, "index.html").toString())


    classes.forEach { cls ->
        val outputDir = File(projectDataPath, cls)
        val htmlBuilderForCls = HtmlBuilder(pathToStyle = listOf("../css/coverage.css", "../css/highlight-idea.css"),
                                            pathToJs = listOf("https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.5.1/highlight.min.js"))
        val filteredStatistics = statistics.map {
            it.perMethod.filter { it.key.contains(cls) }
        }

        htmlBuilderForCls.addTable(
            filteredStatistics.map { it.map { it.key to it.value.last().last() * 100 }.toMap() },
            selectors,
            colors,
            "method"
        )
        htmlBuilderForCls.addFigure(
            FigureBuilders.buildSeveralLinesPlot(
                statistics.map { it.perClass[cls]?.map { it[0] / 1e9 }?.toDoubleArray() ?: doubleArrayOf() },
                statistics.map { it.perClass[cls]?.map { it[1] * 100 }?.toDoubleArray() ?: doubleArrayOf() },
                colors,
                selectors,
                xLabel = "Time (sec)",
                yLabel = "Coverage (%)",
                title = cls
            )
        )

        File(outputDir.toString()).mkdir()
        htmlBuilderForCls.saveHTML(File(outputDir, "index.html").toString())

        filteredStatistics.first().keys.forEach { method ->
            val htmlBuilderForMethod = HtmlBuilder(pathToStyle = listOf("../../css/coverage.css", "../../css/highlight-idea.css"),
                                                    pathToJs = listOf("https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.5.1/highlight.min.js"))
            htmlBuilderForMethod.addFigure(
                FigureBuilders.buildSeveralLinesPlot(
                    filteredStatistics.map { it[method]?.map { it[0] / 1e9 }?.toDoubleArray() ?: doubleArrayOf() },
                    filteredStatistics.map { it[method]?.map { it[1] * 100 }?.toDoubleArray() ?: doubleArrayOf() },
                    colors,
                    selectors,
                    xLabel = "Time (sec)",
                    yLabel = "Coverage (%)",
                    title = method
                )
            )

            File(outputDir, method).mkdirs()
            htmlBuilderForMethod.saveHTML(File(outputDir, "${method}/index.html").toString())
        }
    }
}