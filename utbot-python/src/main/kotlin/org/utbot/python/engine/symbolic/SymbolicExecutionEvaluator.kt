package org.utbot.python.engine.symbolic

import mu.KotlinLogging
import org.utbot.framework.plugin.api.*
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.coverage.CoverageIdGenerator
import org.utbot.python.coverage.buildCoverage
import org.utbot.python.engine.*
import org.utbot.python.engine.utils.transformModelList
import org.utbot.python.evaluation.*
import org.utbot.python.evaluation.serialization.toPythonTree
import org.utbot.python.framework.api.python.PythonSymbolicUtExecution
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.utils.camelToSnakeCase
import org.utbot.summary.fuzzer.names.TestSuggestedInfo
import java.net.ServerSocket
import java.net.SocketException
import java.util.*
import java.util.concurrent.TimeoutException
import kotlin.math.min

private val logger = KotlinLogging.logger {}

class SymbolicExecutionEvaluator(
    val method: PythonMethod,
    val configuration: PythonTestGenerationConfig,
    val executionStorage: ExecutionStorage,
    val until: Long,
    var receiverFinished: Boolean = false
): Thread() {
    private lateinit var serverSocket: ServerSocket
    private lateinit var manager: PythonWorkerManager
    val executionQueue: MutableList<String> = Collections.synchronizedList(mutableListOf<String>())

    init {
        connect()
    }

    private fun connect() {
        try {
            serverSocket = ServerSocket(0)
            manager =
                PythonWorkerManager(
                    serverSocket,
                    configuration.pythonPath,
                    until,
                    configuration.coverageMeasureMode,
                    configuration.sendCoverageContinuously,
                    configuration.doNotGenerateStateAssertions,
                ) {
                    PythonCodeSocketExecutor(
                        method,
                        configuration.testFileInformation.moduleName,
                        configuration.pythonPath,
                        configuration.sysPathDirectories,
                        min(configuration.timeoutForRun, until - System.currentTimeMillis()),
                        it,
                    )
                }
        } catch (_: TimeoutException) {
            close()
        }
    }


    private fun receivePickledInputValuesWithFeedback(pickledTuple: String): ExecutionFeedback? {
        if (System.currentTimeMillis() >= until)
            return null
        try {
            logger.debug("SYMBOLIC running $pickledTuple")
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
            logger.debug { "Symbolic process was interrupted by timeout" }
            return null
        } catch (_: SocketException) {
            connect()
            return null
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
        val summary = emptyList<String>()  // TODO: improve
        val hasThisObject = method.hasThisArgument
        val resultModel = evaluationResult.stateAfter.getById(evaluationResult.resultId).toPythonTree(evaluationResult.stateAfter)

        if (evaluationResult.isException && (resultModel.type.name in configuration.prohibitedExceptions)) {  // wrong type (sometimes mypy fails)
            val errorMessage = "Evaluation with prohibited exception. Error: $resultModel"
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

    fun close() {
        manager.shutdown()
        serverSocket.close()
    }

    override fun run() {
        while (System.currentTimeMillis() < until && (!receiverFinished || executionQueue.isNotEmpty())) {
            val pickledTuple = if (executionQueue.isNotEmpty()) {
                executionQueue.removeFirst()
            } else {
                sleep(10)
                continue
            }
            receivePickledInputValuesWithFeedback(pickledTuple)?.let {
                executionStorage.saveSymbolicExecution(it)
            }
        }
    }
}