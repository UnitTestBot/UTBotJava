package org.utbot.monitoring

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.utbot.common.ThreadBasedExecutor
import org.utbot.common.measureTime
import org.utbot.common.info
import org.utbot.contest.ContestEstimatorJdkInfoProvider
import org.utbot.contest.ContextManager
import org.utbot.contest.GlobalStats
import org.utbot.contest.Paths
import org.utbot.contest.Tool
import org.utbot.contest.runEstimator
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.instrumentation.ConcreteExecutor

private val javaHome = System.getenv("JAVA_HOME")
private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    val outputFile = args.getOrNull(0)?.let { File(it) }

    logger.info { "Monitoring Settings:\n$MonitoringSettings" }

    val methodFilter: String?
    val processedClassesThreshold = MonitoringSettings.processedClassesThreshold
    val timeLimit = MonitoringSettings.classTimeoutSeconds

    val projects = listOf(MonitoringSettings.project)
    methodFilter = null

    val runTimeout = TimeUnit.MINUTES.toMillis(MonitoringSettings.runTimeoutMinutes.toLong())

    val measurementResults = mutableListOf<MonitoringReport>()

    JdkInfoService.jdkInfoProvider = ContestEstimatorJdkInfoProvider(javaHome)
    val executor = ThreadBasedExecutor()

    MonitoringSettings.fuzzingRatios.forEach { fuzzingRatio ->
        val estimatorArgs: Array<String> = arrayOf(
            Paths.classesLists,
            Paths.jarsDir,
            "$timeLimit",
            "$fuzzingRatio",
            Paths.outputDir,
            Paths.moduleTestDir
        )

        logger.info().measureTime({ "Run UTBot generation [fuzzing ratio = $fuzzingRatio]" }) {
            val start = System.nanoTime()

            executor.invokeWithTimeout(runTimeout) {
                runEstimator(
                    estimatorArgs, methodFilter,
                    projects, processedClassesThreshold, listOf(Tool.UtBot)
                )
            }?.onSuccess {
                it as GlobalStats
                it.duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
                measurementResults.add(MonitoringReport(
                    parameters = MonitoringParameters(
                        fuzzingRatio, MonitoringSettings.classTimeoutSeconds, MonitoringSettings.runTimeoutMinutes
                    ),
                    stats = it
                ))
            }?.onFailure { logger.error(it) { "Run failure!" } }
                ?: logger.info { "Run timeout!" }

        }

        ContextManager.cancelAll()
        ConcreteExecutor.defaultPool.forceTerminateProcesses()
        System.gc()
    }
    if (measurementResults.isEmpty())
        exitProcess(1)

    outputFile?.let { file ->
        val format = Json { prettyPrint = true }
        val jsonString = format.encodeToString(measurementResults)
        file.writeText(jsonString)
    }
}
