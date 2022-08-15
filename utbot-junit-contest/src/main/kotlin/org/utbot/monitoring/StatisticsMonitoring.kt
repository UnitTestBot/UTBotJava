package org.utbot.monitoring

import java.io.File
import java.util.concurrent.TimeUnit
import mu.KotlinLogging
import org.utbot.common.ThreadBasedExecutor
import org.utbot.common.bracket
import org.utbot.common.info
import org.utbot.contest.ContestEstimatorJdkInfoProvider
import org.utbot.contest.GlobalStats
import org.utbot.contest.Paths
import org.utbot.contest.Tool
import org.utbot.contest.runEstimator
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.instrumentation.ConcreteExecutor
import kotlin.system.exitProcess

private val javaHome = System.getenv("JAVA_HOME")
private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    logger.info { "Monitoring Settings:\n$MonitoringSettings" }

    val methodFilter: String?
    val projectFilter: List<String>?
    val processedClassesThreshold = MonitoringSettings.processedClassesThreshold
    val tools: List<Tool> = listOf(Tool.UtBot)
    val timeLimit = MonitoringSettings.timeLimitPerClass

    projectFilter = listOf("guava")
    methodFilter = null

    val outputFile = args.getOrNull(0)?.let { File(it) }
    val runTries = MonitoringSettings.runTries
    val runTimeout = TimeUnit.MINUTES.toMillis(MonitoringSettings.runTimeoutMinutes.toLong())

    val estimatorArgs: Array<String> = arrayOf(
        Paths.classesLists,
        Paths.jarsDir,
        "$timeLimit",
        Paths.outputDir,
        Paths.moduleTestDir
    )

    val statistics = mutableListOf<GlobalStats>()

    JdkInfoService.jdkInfoProvider = ContestEstimatorJdkInfoProvider(javaHome)
    val executor = ThreadBasedExecutor()

    repeat(runTries) { idx ->
        logger.info().bracket("Run UTBot try number ${idx + 1}") {

            executor.invokeWithTimeout(runTimeout) {
                runEstimator(estimatorArgs, methodFilter, projectFilter, processedClassesThreshold, tools)
            }
                ?.onSuccess { statistics.add(it as GlobalStats) }
                ?.onFailure { logger.error(it) { "Run failure!" } }
                ?: run {
                    logger.info { "Run timeout!" }
                    ConcreteExecutor.defaultPool.forceTerminateProcesses()
                }

        }
    }

    if (statistics.isEmpty())
        exitProcess(1)

    outputFile?.writeText(statistics.jsonString())
}

private fun StringBuilder.tab(tabs: Int) {
    append("\t".repeat(tabs))
}

private fun StringBuilder.addValue(name: String, value: Any, tabs: Int = 0, needComma: Boolean = true) {
    tab(tabs)
    append("\"$name\": $value")
    if (needComma)
        append(',')
    appendLine()
}

private fun GlobalStats.jsonString(baseTabs: Int = 0) =
    buildString {
        tab(baseTabs)
        appendLine("{")

        val tabs = baseTabs + 1
        addValue("classes_for_generation", classesForGeneration, tabs)
        addValue("testcases_generated", testCasesGenerated, tabs)
        addValue("classes_without_problems", classesWithoutProblems, tabs)
        addValue("classes_canceled_by_timeout", classesCanceledByTimeout, tabs)
        addValue("total_methods_for_generation", totalMethodsForGeneration, tabs)
        addValue("methods_with_at_least_one_testcase_generated", methodsWithAtLeastOneTestCaseGenerated, tabs)
        addValue("methods_with_exceptions", methodsWithExceptions, tabs)
        addValue("suspicious_methods", suspiciousMethods, tabs)
        addValue("test_classes_failed_to_compile", testClassesFailedToCompile, tabs)
        addValue("covered_instructions_count", coveredInstructionsCount, tabs)
        addValue("covered_instructions_count_by_fuzzing", coveredInstructionsCountByFuzzing, tabs)
        addValue("covered_instructions_count_by_concolic", coveredInstructionsCountByConcolic, tabs)
        addValue("total_instructions_count", totalInstructionsCount, tabs)
        addValue("avg_coverage", avgCoverage, tabs, needComma = false)

        tab(baseTabs)
        append("}")
    }

private fun List<GlobalStats>.jsonString() =
    joinToString(
        separator = ",\n",
        prefix = "[\n",
        postfix = "\n]"
    ) {
        it.jsonString(baseTabs = 1)
    }
