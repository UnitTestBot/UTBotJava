package org.utbot.python.evaluation

import mu.KotlinLogging
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.python.FunctionArguments
import org.utbot.python.utils.TemporaryFileManager
import org.utbot.python.utils.getResult
import org.utbot.python.utils.startProcess
import java.lang.Long.max
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

private val logger = KotlinLogging.logger {}
const val ADDITIONAL_EXECUTION_TIME = 250

class PythonWorkerManager(
    private val serverSocket: ServerSocket,
    val pythonPath: String,
    val until: Long,
    val pythonCodeExecutorConstructor: (PythonWorker) -> PythonCodeExecutor,
    private val timeoutForRun: Int
) {
    private val logfile = TemporaryFileManager.createTemporaryFile("","utbot_executor.log", "log", true)

    var timeout: Long = 0
    lateinit var process: Process
    private lateinit var workerSocket: Socket
    private lateinit var codeExecutor: PythonCodeExecutor

    init {
        connect()
    }

    fun connect() {
        val processStartTime = System.currentTimeMillis()
        process = startProcess(listOf(
            pythonPath,
            "-m", "utbot_executor",
            "localhost",
            serverSocket.localPort.toString(),
            "--logfile", logfile.absolutePath,
            "--loglevel", "DEBUG",  // "DEBUG", "INFO", "WARNING", "ERROR"
        ))
        timeout = max(until - processStartTime, 0)
        workerSocket = try {
            serverSocket.soTimeout = timeout.toInt()
            serverSocket.accept()
        } catch (e: SocketTimeoutException) {
            val result = getResult(process, max(until - processStartTime, 0))
            logger.info("utbot_executor exit value: ${result.exitValue}. stderr: ${result.stderr}, stdout: ${result.stdout}.")
            process.destroy()
            throw TimeoutException("Worker not connected")
        }
        logger.debug { "Worker connected successfully" }

//        workerSocket.soTimeout = timeoutForRun + ADDITIONAL_EXECUTION_TIME  // TODO: maybe +eps for serialization/deserialization?
        val pythonWorker = PythonWorker(workerSocket)
        codeExecutor = pythonCodeExecutorConstructor(pythonWorker)
    }

    fun disconnect() {
        workerSocket.close()
        process.destroy()
    }

    fun reconnect() {
        disconnect()
        connect()
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
}