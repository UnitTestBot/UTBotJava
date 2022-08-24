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
    val processedClassesThreshold = MonitoringSettings.processedClassesThreshold
    val tools: List<Tool> = listOf(Tool.UtBot)
    val timeLimit = MonitoringSettings.classTimeoutSeconds

    val project = MonitoringSettings.project
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
            val start = System.nanoTime()

            executor.invokeWithTimeout(runTimeout) {
                runEstimator(
                    estimatorArgs, methodFilter,
                    listOf(project), processedClassesThreshold, tools
                )
            }
                ?.onSuccess {
                    it as GlobalStats
                    it.duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
                    statistics.add(it)
                }
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

private fun StringBuilder.addValue(name: String, value: Any?, tabs: Int = 0, needComma: Boolean = true) {
    tab(tabs)
    append("\"$name\": $value")
    if (needComma) append(',')
    appendLine()
}


private fun objectString(vararg values: Pair<String, Any?>, baseTabs: Int = 0, needFirstTab: Boolean = false) =
    buildString {
        if (needFirstTab) tab(baseTabs)
        appendLine("{")

        val tabs = baseTabs + 1
        values.forEachIndexed { index, (name, value) ->
            addValue(name, value, tabs, index != values.lastIndex)
        }

        tab(baseTabs)
        append("}")
    }

private fun arrayString(vararg values: Any?, baseTabs: Int = 0, needFirstTab: Boolean = false) =
    buildString {
        if (needFirstTab) tab(baseTabs)
        appendLine("[")

        val tabs = baseTabs + 1
        values.forEachIndexed { index, value ->
            tab(tabs)
            append(value)
            if (index != values.lastIndex) append(",")
            appendLine()
        }

        tab(baseTabs)
        append("]")
    }


private fun GlobalStats.parametersObject(baseTabs: Int = 0, needFirstTab: Boolean = false) =
    objectString(
        "target" to "\"${MonitoringSettings.project}\"",
        "class_timeout_sec" to MonitoringSettings.classTimeoutSeconds,
        "run_timeout_min" to MonitoringSettings.runTimeoutMinutes,
        baseTabs = baseTabs,
        needFirstTab = needFirstTab
    )


private fun GlobalStats.metricsObject(baseTabs: Int = 0, needFirstTab: Boolean = false) =
    objectString(
        "duration_ms" to duration,
        "classes_for_generation" to classesForGeneration,
        "testcases_generated" to testCasesGenerated,
        "classes_without_problems" to classesWithoutProblems,
        "classes_canceled_by_timeout" to classesCanceledByTimeout,
        "total_methods_for_generation" to totalMethodsForGeneration,
        "methods_with_at_least_one_testcase_generated" to methodsWithAtLeastOneTestCaseGenerated,
        "methods_with_exceptions" to methodsWithExceptions,
        "suspicious_methods" to suspiciousMethods,
        "test_classes_failed_to_compile" to testClassesFailedToCompile,
        "covered_instructions" to coveredInstructions,
        "covered_instructions_by_fuzzing" to coveredInstructionsByFuzzing,
        "covered_instructions_by_concolic" to coveredInstructionsByConcolic,
        "total_instructions" to totalInstructions,
        "avg_coverage" to avgCoverage,
        baseTabs = baseTabs,
        needFirstTab = needFirstTab
    )

private fun GlobalStats.jsonString(baseTabs: Int = 0, needFirstTab: Boolean = false) =
    objectString(
        "parameters" to parametersObject(baseTabs + 1),
        "metrics" to metricsObject(baseTabs + 1),
        baseTabs = baseTabs,
        needFirstTab = needFirstTab
    )

private fun Iterable<GlobalStats>.jsonString() =
    arrayString(*map {
        it.jsonString(baseTabs = 1)
    }.toTypedArray())
