package org.utbot.monitoring

import java.io.File
import org.utbot.contest.ContestEstimatorJdkPathProvider
import org.utbot.contest.GlobalStats
import org.utbot.contest.Paths
import org.utbot.contest.Tool
import org.utbot.contest.runEstimator
import org.utbot.contest.toText
import org.utbot.framework.JdkPathService

private val javaHome = System.getenv("JAVA_HOME")

fun main(args: Array<String>) {
    val methodFilter: String?
    val projectFilter: List<String>?
    val processedClassesThreshold = 9999
    val tools: List<Tool> = listOf(Tool.UtBot)
    val timeLimit = 20

    require(args.size == 1) {
        "Wrong arguments: <output json> expected, but got: ${args.toText()}"
    }

    projectFilter = listOf("guava")
    methodFilter = null

    val outputFile = File(args[0])

    val estimatorArgs: Array<String> = arrayOf(
        Paths.classesLists,
        Paths.jarsDir,
        "$timeLimit",
        Paths.outputDir,
        Paths.moduleTestDir
    )

    JdkPathService.jdkPathProvider = ContestEstimatorJdkPathProvider(javaHome)
    val statistics = runEstimator(estimatorArgs, methodFilter, projectFilter, processedClassesThreshold, tools)
    statistics.dumpJson(outputFile)
}

private fun StringBuilder.addValue(name: String, value: Any, tabs: Int = 0, needComma: Boolean = true) {
    append("\t".repeat(tabs))
    append("\"$name\": $value")
    if (needComma)
        append(',')
    appendLine()
}

private fun GlobalStats.dumpJson(file: File) {
    val jsonString = buildString {
        appendLine("{")

        val tabs = 1
        addValue("classes_for_generation", classesForGeneration, tabs)
        addValue("tc_generated", testCasesGenerated, tabs)
        addValue("classes_without_problems", classesWithoutProblems, tabs)
        addValue("classes_canceled_by_timeout", classesCanceledByTimeout, tabs)
        addValue("total_methods_for_generation", totalMethodsForGeneration, tabs)
        addValue("methods_with_at_least_one_testcase_generated", methodsWithAtLeastOneTestCaseGenerated, tabs)
        addValue("methods_with_exceptions", methodsWithExceptions, tabs)
        addValue("suspicious_methods", suspiciousMethods, tabs)
        addValue("test_classes_failed_to_compile", testClassesFailedToCompile, tabs)
        addValue("covered_instructions_count", coveredInstructionsCount, tabs)
        addValue("total_instructions_count", totalInstructionsCount, tabs)
        addValue("avg_coverage", avgCoverage, tabs, needComma = false)

        appendLine("}")
    }
    file.writeText(jsonString)
}
