@file:Suppress("NestedLambdaShadowedImplicitParameter")

package org.utbot.contest

import java.io.File

fun main(args: Array<String>) {
    val fileToParse = args.singleOrNull() ?: error("")

    // csv file with a following structure
    /*
    name                             ,run1       ,         ,run2,      ,...
                                     ,line       ,branch   ,line,branch,...
    com.google.common.base.CaseFormat,28% (19/67),6% (2/30),28% (19/67),6% (2/30),...
     */
    val file = File(fileToParse)

    val (names, valuesForClasses) = file.readLines()
        // lines with "name,run1,run2" and ",line,branch,line..."
        .drop(2)
        // get values but empty elements that might occur because of wrong import from excel
        .map { it.split(",").dropLastWhile { it.isEmpty() } }
        .let { it.map { it.first() } to it.map { it.drop(1) } }

    val linesForClasses = Array<MutableList<String>>(valuesForClasses.size) { mutableListOf() }
    val branchesForClasses = Array<MutableList<String>>(valuesForClasses.size) { mutableListOf() }

    // class1 33%(10/30) 33%(1/3) 67%(20/30) 67%(2/3) 100%(30/30) 100%(3/3) ->
    // linesForClasses[class1] == listOf(10/30, 20/30, 30/30) && branchesForClasses[class1] == listOf(1/3, 2/3, 3/3)
    valuesForClasses.mapIndexed { classIndex, valuesForClass ->
        valuesForClass
            .map { it.coveredTotalRate() }
            .forEachIndexed { index, value ->
                // add here because of ambiguous call of the operator plus
                if (index % 2 == 0) linesForClasses[classIndex].add(value) else branchesForClasses[classIndex].add(value)
            }
    }

    // linesForClasses[class1] == listOf(listOf(10, 20, 30), listOf(30, 30, 30)) &&
    // branchesForClasses[class1] == listOf(listOf(1, 2, 3), listOf(3, 3, 3))
    val totalLinesWithZeros = linesForClasses.extractCoveredToTotal()
    val totalBranchesWithZeros = branchesForClasses.extractCoveredToTotal()

    // calculate for each class its totalCovered*.average() / total*.average()
    val averageLines = totalLinesWithZeros.mapAverage()
    val averageBranches = totalBranchesWithZeros.mapAverage()

    val results = names.mapIndexed { i, s ->
        CalculationResult(s, averageLines[i], averageBranches[i])
    }

    val averageLinesByClassesWithZeros = results.map { it.averageLines }.average()
    val averageBranchesByClassesWithZeros = results.map { it.averageBranches }.average()

    File(args.first().let { it.substringBeforeLast(".") + "_result.txt" })
        .bufferedWriter()
        .use { writer ->
            writer.write("Average line coverage per class (CE counts as zero coverage): $averageLinesByClassesWithZeros\n")
            writer.write("Average branch coverage per class (CE counts as zero coverage): $averageBranchesByClassesWithZeros\n")

            writer.write("\n")

            writer.write("Results per classes (CE counts as zero coverage):\n")
            results.forEach {
                writer.write(
                    "${it.name}: " +
                            "average lines ${it.averageLines.format(2)} | " +
                            "average branches ${it.averageBranches.format(2)}\n"
                )
            }
        }
}

private fun Array<MutableList<String>>.extractCoveredToTotal() =
    map { linesForClass ->
        linesForClass
            .asSequence()
            .map { it.split("/").let { it.first().toInt() to it.last().toInt() } }
            .let { it.map { it.first }.toList() to it.map { it.second }.toList() }
    }

private data class CalculationResult(
    val name: String,
    val averageLines: Double,
    val averageBranches: Double
)

private fun String.coveredTotalRate(): String = substringAfter("(").takeWhile { it != ')' }

private fun Double.format(digits: Int) = "%.${digits}f".format(this)

private fun List<Pair<List<Int>, List<Int>>>.mapAverage(): List<Double> =
    map { it.first.average() / it.second.average() }