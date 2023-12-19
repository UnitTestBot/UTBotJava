package org.utbot.python.evaluation

import mu.KotlinLogging
import org.apache.logging.log4j.LogManager
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.python.FunctionArguments
import org.utbot.python.PythonMethod
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.coverage.PythonCoverageMode
import org.utbot.python.utils.TemporaryFileManager
import org.utbot.python.utils.getResult
import org.utbot.python.utils.startProcess
import java.lang.Long.max
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.math.min

private val logger = KotlinLogging.logger {}

class PythonWorkerManager(
    private val serverSocket: ServerSocket,
    val pythonPath: String,
    val until: Long,
    private val coverageMeasureMode: PythonCoverageMode = PythonCoverageMode.Instructions,
    private val sendCoverageContinuously: Boolean = true,
    private val doNotGenerateStateAssertions: Boolean = false,
    val pythonCodeExecutorConstructor: (PythonWorker) -> PythonCodeExecutor,
) {
    constructor(
        method: PythonMethod,
        serverSocket: ServerSocket,
        configuration: PythonTestGenerationConfig,
        until: Long,
    ) : this(
        serverSocket,
        configuration.pythonPath,
        until,
        configuration.coverageMeasureMode,
        configuration.sendCoverageContinuously,
        configuration.doNotGenerateStateAssertions,
        {
            PythonCodeSocketExecutor(
                method,
                configuration.testFileInformation.moduleName,
                configuration.pythonPath,
                configuration.sysPathDirectories,
                min(configuration.timeoutForRun, until - System.currentTimeMillis()),
                it,
            )
        }
    )

    var timeout: Long = 0
    lateinit var process: Process
    private lateinit var workerSocket: Socket
    private lateinit var codeExecutor: PythonCodeExecutor

    val coverageReceiver = PythonCoverageReceiver(until)

    init {
        coverageReceiver.start()
        connect()
    }

    private fun connect() {
        val processStartTime = System.currentTimeMillis()
        if (this::process.isInitialized && process.isAlive) {
            process.destroy()
        }
        val logLevel = LogManager.getRootLogger().level.name()
        if (serverSocket.isClosed) {
            serverSocket.accept()
        }
        process = startProcess(
            listOf(
                pythonPath,
                "-m", "utbot_executor",
                "localhost",
                serverSocket.localPort.toString(),
                coverageReceiver.address().first,
                coverageReceiver.address().second,
                "--logfile", logfile.absolutePath,
                "--loglevel", logLevel,  // "DEBUG", "INFO", "WARNING", "ERROR"
                "--coverage_type", coverageMeasureMode.toString(),  // "lines", "instructions"
                sendCoverageContinuously.toSendCoverageContinuouslyString(),  // "--send_coverage", "--no-send_coverage"
                doNotGenerateStateAssertions.toDoNotGenerateStateAssertionsString(), // "--generate_state_assertions", // "--no-generate_state_assertions"
            )
        )
        timeout = max(until - processStartTime, 0)
        if (this::workerSocket.isInitialized && !workerSocket.isClosed) {
            workerSocket.close()
        }
        workerSocket = try {
            serverSocket.soTimeout = timeout.toInt()
            serverSocket.accept()
        } catch (e: SocketTimeoutException) {
            val result = getResult(process, max(until - processStartTime, 0))
            logger.debug { "utbot_executor exit value: ${result.exitValue}. stderr: ${result.stderr}, stdout: ${result.stdout}." }
            process.destroy()
            throw TimeoutException("Worker not connected")
        } catch (e: SocketException) {
            logger.debug { e.message }
            throw SocketException("Worker not connected: $e")
        }
        logger.debug { "Worker connected successfully" }

//        workerSocket.soTimeout = timeoutForRun  // TODO: maybe +eps for serialization/deserialization?
        val pythonWorker = PythonWorker(workerSocket)
        codeExecutor = pythonCodeExecutorConstructor(pythonWorker)
    }

    fun disconnect() {
        workerSocket.close()
        process.destroyForcibly()
    }

    private fun reconnect() {
        disconnect()
        connect()
    }

    fun shutdown() {
        disconnect()
        coverageReceiver.kill()
    }

    fun runWithCoverage(
        fuzzedValues: FunctionArguments,
        additionalModulesToImport: Set<String>,
        coverageId: String
    ): PythonEvaluationResult {
        val evaluationResult = try {
            codeExecutor.runWithCoverage(fuzzedValues, additionalModulesToImport, coverageId)
        } catch (_: SocketTimeoutException) {
            logger.debug { "Socket timeout" }
            reconnect()
            PythonEvaluationTimeout()
        }
        if (evaluationResult is PythonEvaluationError || evaluationResult is PythonEvaluationTimeout) {
            reconnect()
        }
        return evaluationResult
    }

    fun runWithCoverage(
        pickledArguments: String,
        coverageId: String
    ): PythonEvaluationResult {
        val evaluationResult = try {
            codeExecutor.runWithCoverage(pickledArguments, coverageId)
        } catch (_: SocketTimeoutException) {
            logger.debug { "Socket timeout" }
            reconnect()
            PythonEvaluationTimeout()
        }
        if (evaluationResult is PythonEvaluationError || evaluationResult is PythonEvaluationTimeout) {
            reconnect()
        }
        return evaluationResult
    }

    fun run(
        fuzzedValues: FunctionArguments,
        additionalModulesToImport: Set<String>
    ): PythonEvaluationResult {
        val evaluationResult = try {
            codeExecutor.run(fuzzedValues, additionalModulesToImport)
        } catch (_: SocketTimeoutException) {
            logger.debug { "Socket timeout" }
            reconnect()
            PythonEvaluationTimeout()
        }
        if (evaluationResult is PythonEvaluationError) {
            reconnect()
        }
        return evaluationResult
    }

    companion object {
        val logfile = TemporaryFileManager.createTemporaryFile("", "utbot_executor.log", "log", true)

        fun Boolean.toSendCoverageContinuouslyString(): String {
            return if (this) {
                "--send_coverage"
            } else {
                "--no-send_coverage"
            }
        }

        fun Boolean.toDoNotGenerateStateAssertionsString(): String {
            return if (this) {
                "--no-generate_state_assertions"
            } else {
                "--generate_state_assertions"
            }
        }
    }
}
