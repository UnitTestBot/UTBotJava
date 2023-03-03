package org.utbot.python.evaluation

import mu.KotlinLogging
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.python.FunctionArguments
import org.utbot.python.utils.TemporaryFileManager
import org.utbot.python.utils.getResult
import org.utbot.python.utils.startProcess
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

class PythonWorkerManager(
    val serverSocket: ServerSocket,
    val pythonPath: String,
    val until: Long,
    val pythonCodeExecutorConstructor: (PythonWorker) -> PythonCodeExecutor
) {
    val logfile = TemporaryFileManager.createTemporaryFile("","utbot_executor.log", "log", true)

    var timeout: Long = 0
    lateinit var process: Process
    lateinit var workerSocket: Socket
    lateinit var codeExecutor: PythonCodeExecutor

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
            //"--loglevel", "DEBUG",
        ))
        timeout = until - processStartTime
        workerSocket = try {
            serverSocket.soTimeout = timeout.toInt()
            serverSocket.accept()
        } catch (e: SocketTimeoutException) {
            val result = getResult(process, 10)
            logger.info("utbot_executor exit value: ${result.exitValue}. stderr: ${result.stderr}.")
            process.destroy()
            throw TimeoutException("Worker not connected")
        }
        logger.debug { "Worker connected successfully" }

        workerSocket.soTimeout = 2000
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