package org.utbot.python.engine.symbolic

import mu.KotlinLogging
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.coverage.CoverageIdGenerator
import org.utbot.python.coverage.PyInstruction
import org.utbot.python.coverage.buildCoverage
import org.utbot.python.engine.ExecutionFeedback
import org.utbot.python.engine.InvalidExecution
import org.utbot.python.engine.TypeErrorFeedback
import org.utbot.python.engine.ValidExecution
import org.utbot.python.engine.utils.transformModelList
import org.utbot.python.evaluation.PythonCodeSocketExecutor
import org.utbot.python.evaluation.PythonEvaluationError
import org.utbot.python.evaluation.PythonEvaluationSuccess
import org.utbot.python.evaluation.PythonEvaluationTimeout
import org.utbot.python.evaluation.PythonWorkerManager
import org.utbot.python.evaluation.serialization.toPythonTree
import org.utbot.python.framework.api.python.PythonSymbolicUtExecution
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.utils.camelToSnakeCase
import org.utbot.summary.fuzzer.names.TestSuggestedInfo
import java.net.ServerSocket
import java.util.concurrent.TimeoutException

private val logger = KotlinLogging.logger {}

class USVMPythonAnalysisResultReceiver(
    val method: PythonMethod,
    val configuration: PythonTestGenerationConfig,
    val until: Long,
) {
    private val serverSocket = ServerSocket(0)
    private val manager =
        PythonWorkerManager(
            serverSocket,
            configuration.pythonPath,
            until,
            configuration.coverageMeasureMode,
            configuration.sendCoverageContinuously,
        ) {
            PythonCodeSocketExecutor(
                method,
                configuration.testFileInformation.moduleName,
                configuration.pythonPath,
                configuration.sysPathDirectories,
                configuration.timeoutForRun,
                it,
            )
        }

    fun receivePickledInputValues(pickledTuple: String): ExecutionFeedback? {
        serverSocket.use {
            try {
                val coverageId = CoverageIdGenerator.createId()
                return when (
                    val evaluationResult = manager.runWithCoverage(pickledTuple, coverageId)
                ) {
                    is PythonEvaluationError -> {
                        val stackTraceMessage = evaluationResult.stackTrace.joinToString("\n")
                        val utError = UtError(
                            "Error evaluation: ${evaluationResult.status}, ${evaluationResult.message}\n${stackTraceMessage}",
                            Throwable(stackTraceMessage)
                        )
                        logger.debug(stackTraceMessage)
                        InvalidExecution(utError)
                    }

                    is PythonEvaluationTimeout -> {
//                        val coveredInstructions = manager.coverageReceiver.coverageStorage.getOrDefault(coverageId, mutableListOf())
//                        handleTimeoutResult(method, coveredInstructions)
                        null
                    }

                    is PythonEvaluationSuccess -> {
                        handleSuccessResult(method, evaluationResult)
                    }
                }
            } catch (_: TimeoutException) {
                logger.debug { "Fuzzing process was interrupted by timeout" }
                return null
            }
        }
    }

    private fun suggestExecutionName(
        method: PythonMethod,
        executionResult: UtExecutionResult
    ): TestSuggestedInfo {
        val testSuffix = when (executionResult) {
            is UtExecutionSuccess -> {
                // can be improved
                method.name
            }
            is UtExecutionFailure -> "${method.name}_with_exception"
            else -> method.name
        }
        val testName = "test_$testSuffix"
        return TestSuggestedInfo(
            testName,
            testName,
        )
    }

    private fun handleSuccessResult(
        method: PythonMethod,
        evaluationResult: PythonEvaluationSuccess
    ): ExecutionFeedback {
        val prohibitedExceptions = listOf(
            "builtins.AttributeError",
            "builtins.TypeError",
            "builtins.NotImplementedError",
        )

        val summary = emptyList<String>()  // TODO: improve
        val hasThisObject = method.hasThisArgument
        val resultModel = evaluationResult.stateAfter.getById(evaluationResult.resultId).toPythonTree(evaluationResult.stateAfter)

        if (evaluationResult.isException && (resultModel.type.name in prohibitedExceptions)) {  // wrong type (sometimes mypy fails)
            val errorMessage = "Evaluation with prohibited exception. Substituted types: ???"  // TODO: improve message
            logger.debug { errorMessage }
            return TypeErrorFeedback(errorMessage)
        }
        val executionResult =
            if (evaluationResult.isException) {
                UtImplicitlyThrownException(Throwable(resultModel.type.toString()), false)
            }
            else {
                UtExecutionSuccess(PythonTreeModel(resultModel))
            }
        val testMethodName = suggestExecutionName(method, executionResult)

        val (beforeThisObject, beforeModelList) = transformModelList(hasThisObject, evaluationResult.stateBefore, evaluationResult.modelListIds)
        val (afterThisObject, afterModelList) = transformModelList(hasThisObject, evaluationResult.stateAfter, evaluationResult.modelListIds)

        val utFuzzedExecution = PythonSymbolicUtExecution(
            stateBefore = EnvironmentModels(beforeThisObject, beforeModelList, emptyMap(), executableToCall = null),
            stateAfter = EnvironmentModels(afterThisObject, afterModelList, emptyMap(), executableToCall = null),
            diffIds = evaluationResult.diffIds,
            result = executionResult,
            coverage = buildCoverage(evaluationResult.coveredStatements, evaluationResult.missedStatements),
            testMethodName = testMethodName.testName?.camelToSnakeCase(),
            displayName = testMethodName.displayName,
            summary = summary.map { DocRegularStmt(it) },
            arguments = method.argumentsWithoutSelf,
        )
        return ValidExecution(utFuzzedExecution)
    }

    private fun handleTimeoutResult(
        method: PythonMethod,
        coveredInstructions: MutableList<PyInstruction>
    ): ExecutionFeedback {
        TODO()
    }
}