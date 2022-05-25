package org.utbot

import org.utbot.QualityAnalysisConfig
import org.utbot.visual.FigureBuilders
import org.utbot.visual.HtmlBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.File
import java.nio.file.Paths


data class Coverage(val misInstructions: Int, val covInstruction: Int, val misBranches: Int, val covBranches: Int, val time: Double = 0.0) {
    fun getInstructionCoverage(): Double = when {
        (this.misInstructions == 0 && this.covInstruction == 0) -> 0.0
        (this.misInstructions == 0) -> 1.0
        else -> this.covInstruction.toDouble() / (this.covInstruction + this.misInstructions)
    }

    fun getBranchesCoverage(): Double = when {
        (this.misBranches == 0 && this.covBranches == 0) -> 0.0
        (this.misBranches == 0) -> 1.0
        else -> this.covBranches.toDouble() / (this.covBranches + this.misBranches)
    }

    fun getInstructions() = misInstructions + covInstruction
}


fun parseJacocoReport(path: String, classes: Set<String>): Pair<Map<String, Coverage>, Map<String, Coverage>> {
    val perClassResult = mutableMapOf<String, Coverage>()
    val perMethodResult = mutableMapOf<String, Coverage>()
    val contestDocument: Document = Jsoup.parse(File("$path/index.html"), null)
    val pkgRows: Elements = contestDocument.select("table")[0].select("tr")

    for (i in 2 until pkgRows.size) {
        val pkgHref = pkgRows[i].select("td")[0].select("a").attr("href")
        val packageDocument: Document = Jsoup.parse(File("$path/$pkgHref"), null)
        val classRows: Elements = packageDocument.select("table")[0].select("tr")

        for (j in 2 until classRows.size) {
            val classHref = classRows[j].select("td")[0].select("a").attr("href")
            val className = pkgHref.replace("/index.html", "") + "." + classHref.split(".")[0]
            if (!classes.contains(className)) {
                continue
            }

            val classDocument: Document = Jsoup.parse(File("$path/${pkgHref.replace("index.html", classHref)}"), null)
            val methodRows: Elements = classDocument.select("table")[0].select("tr")

            val coverageInfos = methodRows[1].select("td")
            val (misInstructions, covInstructions) = coverageInfos[1].text()?.replace(",", "")?.split(" of ")?.let {
                val misInstructions = it[0].toInt()
                val allInstructions = it[1].toInt()
                misInstructions to (allInstructions - misInstructions)
            } ?: (0 to 0)

            val (misBranches, covBranches) = coverageInfos[3].text()?.replace(",", "")?.split(" of ")?.let {
                val misBranches = it[0].toInt()
                val allBranches = it[1].toInt()
                misBranches to (allBranches - misBranches)
            } ?: (0 to 0)

            val name = pkgHref.replace("/index.html", "") + "." + classHref.split(".")[0]
            perClassResult[name] = Coverage(misInstructions, covInstructions, misBranches, covBranches)

            for (k in 2 until methodRows.size) {

                val cols = methodRows[k].select("td")
                val methodHref = methodRows[k].select("td")[0].select("a").attr("href")
                val methodName = classHref.replace("/index.html", "." + methodHref.replace("html", cols[0].text()))

                val instructions = cols[1].select("img")
                val methodMisInstructions = instructions.getOrNull(0)?.attr("title")?.toString()?.replace(",", "")?.toInt()
                    ?: 0
                val methodCovInstructions = instructions.getOrNull(1)?.attr("title")?.toString()?.replace(",", "")?.toInt()
                    ?: 0

                val branches = cols[3].select("img")
                val methodMisBranches = branches.getOrNull(0)?.attr("title")?.toString()?.replace(",", "")?.toInt() ?: 0
                val methodCovBranches = branches.getOrNull(1)?.attr("title")?.toString()?.replace(",", "")?.toInt() ?: 0

                if (methodMisInstructions == 0 && methodCovBranches == 0 && methodCovInstructions == 0 && methodMisBranches == 0) continue
                perMethodResult[methodName] = Coverage(methodMisInstructions, methodCovInstructions, methodMisBranches, methodCovBranches)
            }
        }
    }

    return perClassResult to perMethodResult
}


fun main() {
    val htmlBuilder = HtmlBuilder()

    val classes = mutableSetOf<String>()
    File(QualityAnalysisConfig.classesList).inputStream().bufferedReader().forEachLine { classes.add(it) }

    // Parse data
    val jacocoCoverage = QualityAnalysisConfig.selectors.map {
        it to parseJacocoReport("eval/jacoco/${QualityAnalysisConfig.project}/${it}",classes).first
    }

    // Instruction coverage report (sum coverages percentages / classNum)
    val instructionMetrics = jacocoCoverage.map { jacoco ->
        jacoco.first to jacoco.second.map { it.value.getInstructionCoverage() }
    }
    htmlBuilder.addHeader("Instructions coverage (sum coverages percentages / classNum)")
    instructionMetrics.forEach {
        htmlBuilder.addText("Mean(Instruction (${it.first} model)) =${it.second.sum() / it.second.size}")
    }
    htmlBuilder.addFigure(
            FigureBuilders.buildBoxPlot(
                instructionMetrics.map { it.second.map { _ -> "Instructions (${it.first} model)" } }.flatten().toTypedArray(),
                instructionMetrics.map { it.second }.flatten().toDoubleArray(),
                title = "Coverage",
                xLabel = "Instructions",
                yLabel = "Count"
            )
    )

    // Instruction coverage report (sum covered instructions / sum instructions)
    htmlBuilder.addHeader("Instructions coverage(sum covered instructions / sum instructions)")
    val covInstruction = jacocoCoverage.map { jacoco ->
        jacoco.first to jacoco.second.map { it.value.covInstruction }
    }
    val instructions = jacocoCoverage.map { jacoco ->
        jacoco.first to jacoco.second.map { it.value.getInstructions() }
    }.toMap()
    covInstruction.forEach {
        htmlBuilder.addText("Mean(Instruction (${it.first} model)) =${it.second.sum().toDouble() / (instructions[it.first]?.sum()?.toDouble() ?: 0.0)}")
    }
    htmlBuilder.addFigure(
        FigureBuilders.buildBoxPlot(
            covInstruction.map { it.second.map { _ -> "Instructions (${it.first} model)" } }.flatten().toTypedArray(),
            covInstruction.map { it.second }.flatten().map { it.toDouble() }.toDoubleArray(),
            title = "Coverage",
            xLabel = "Instructions",
            yLabel = "Count"
        )
    )

    // Branches coverage report
    val branchesMetrics = jacocoCoverage.map { jacoco ->
        jacoco.first to jacoco.second.map { it.value.getBranchesCoverage() }
    }
    htmlBuilder.addHeader("Branches coverage")
    branchesMetrics.forEach {
        htmlBuilder.addText("Mean(Branches (${it.first} model)) =${it.second.sum() / it.second.size}")
    }
    htmlBuilder.addFigure(
        FigureBuilders.buildBoxPlot(
            branchesMetrics.map { it.second.map { it2 -> "Branches (${it.first} model)" } }.flatten().toTypedArray(),
            branchesMetrics.map { it.second }.flatten().toDoubleArray(),
            title = "Coverage",
            xLabel = "Branches",
            yLabel = "Count"
        )
    )

    // Save report
    htmlBuilder.saveHTML(Paths.get(
        QualityAnalysisConfig.outputDir,
        QualityAnalysisConfig.project,
        "test.html"
    ).toFile().absolutePath)
}