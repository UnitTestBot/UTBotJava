package org.utbot.python

import mu.KotlinLogging
import org.utbot.framework.minimization.minimizeExecutions
import org.utbot.framework.plugin.api.UtClusterInfo
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.python.engine.GlobalPythonEngine
import org.utbot.python.framework.api.python.PythonUtExecution
import org.utbot.python.newtyping.mypy.MypyInfoBuild
import org.utbot.python.newtyping.mypy.MypyReportLine

private val logger = KotlinLogging.logger {}
private const val MAX_EMPTY_COVERAGE_TESTS = 5

class PythonTestCaseGenerator(
    private val configuration: PythonTestGenerationConfig,
    private val mypyStorage: MypyInfoBuild,
    private val mypyReportLine: List<MypyReportLine>
) {
    private val withMinimization = configuration.withMinimization

    fun generate(method: PythonMethod, until: Long): List<PythonTestSet> {

        logger.info { "Start test generation for ${method.name}" }
        val engine = GlobalPythonEngine(
            method = method,
            configuration = configuration,
            mypyStorage,
            mypyReportLine,
            until,
        )
        try {
            engine.run()
        } catch (_: OutOfMemoryError) {
            logger.debug { "Out of memory error. Stop test generation process" }
        }

        logger.info { "Collect all test executions for ${method.name}" }
        return listOf(
            buildTestSet(method, engine.executionStorage.fuzzingExecutions, engine.executionStorage.fuzzingErrors, UtClusterInfo("FUZZER")),
            buildTestSet(method, engine.executionStorage.symbolicExecutions, engine.executionStorage.symbolicErrors, UtClusterInfo("SYMBOLIC")),
        )
    }

    private fun buildTestSet(
        method: PythonMethod,
        executions: List<PythonUtExecution>,
        errors: List<UtError>,
        clusterInfo: UtClusterInfo,
    ): PythonTestSet {
        val (emptyCoverageExecutions, coverageExecutions) = executions.partition { it.coverage == null }
        val (successfulExecutions, failedExecutions) = coverageExecutions.partition { it.result is UtExecutionSuccess }
        val minimized  =
            if (withMinimization)
                minimizeExecutions(successfulExecutions) +
                        minimizeExecutions(failedExecutions) +
                        emptyCoverageExecutions.take(MAX_EMPTY_COVERAGE_TESTS)
            else
                coverageExecutions + emptyCoverageExecutions.take(MAX_EMPTY_COVERAGE_TESTS)
        return PythonTestSet(
            method,
            minimized,
            errors,
            executionsNumber = executions.size,
            clustersInfo = listOf(Pair(clusterInfo, minimized.indices))
        )
    }
}
