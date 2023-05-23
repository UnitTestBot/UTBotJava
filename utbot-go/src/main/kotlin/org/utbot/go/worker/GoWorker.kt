package org.utbot.go.worker

import com.beust.klaxon.Klaxon
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.go.api.GoUtFunction
import org.utbot.go.api.util.convertToRawValue
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoUtModel
import org.utbot.go.util.convertObjectToJsonString
import org.utbot.go.util.executeCommandByNewProcessOrFailWithoutWaiting
import org.utbot.go.util.modifyEnvironment
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class GoWorker private constructor(
    private var process: Process,
    private var socket: Socket,
    private val testFunctionName: String,
    private val testFilePath: String,
    private val goPackage: GoPackage,
    private val goExecutableAbsolutePath: Path,
    private val gopathAbsolutePath: Path,
    private val workingDirectory: File,
    private val serverSocket: ServerSocket,
    private val readTimeoutMillis: Long,
    private val connectionTimeoutMillis: Long,
    private val endOfWorkerExecutionTimeoutMillis: Long
) : Closeable {
    private var input: DataInputStream = DataInputStream(socket.getInputStream())
    private var output: DataOutputStream = DataOutputStream(socket.getOutputStream())

    data class TestInput(
        val functionName: String, val arguments: List<RawValue>
    )

    fun sendFuzzedParametersValues(
        function: GoUtFunction, arguments: List<GoUtModel>, aliases: Map<GoPackage, String?>
    ): Int {
        val rawValues = arguments.map { it.convertToRawValue(goPackage, aliases) }
        val testCase = TestInput(function.name, rawValues)
        val json = convertObjectToJsonString(testCase)
        output.write(json.encodeToByteArray())
        output.flush()
        return json.length
    }

    fun receiveRawExecutionResult(): RawExecutionResult {
        socket.soTimeout = readTimeoutMillis.toInt()
        val length = input.readInt()
        val buffer = ByteArray(length)
        input.read(buffer)
        return Klaxon().parse(buffer.toString(StandardCharsets.UTF_8))
            ?: error("Error with parsing json as raw execution result")
    }

    fun restartWorker() {
        socket.close()
        input.close()
        output.close()

        process.destroy()
        process = startWorkerProcess(
            testFunctionName, testFilePath, goExecutableAbsolutePath, gopathAbsolutePath, workingDirectory
        )

        socket = connectingToWorker(
            serverSocket, process, connectionTimeoutMillis, endOfWorkerExecutionTimeoutMillis
        )
        input = DataInputStream(socket.getInputStream())
        output = DataOutputStream(socket.getOutputStream())
    }

    override fun close() {
        socket.close()
        input.close()
        output.close()

        val processHasExited = process.waitFor(endOfWorkerExecutionTimeoutMillis, TimeUnit.MILLISECONDS)
        if (!processHasExited) {
            process.destroy()
            val processOutput = InputStreamReader(process.inputStream).readText()
            throw TimeoutException(buildString {
                appendLine("Timeout exceeded: Worker didn't finish. Process output: ")
                appendLine(processOutput)
            })
        }
        val exitCode = process.exitValue()
        if (exitCode != 0) {
            val processOutput = InputStreamReader(process.inputStream).readText()
            throw RuntimeException(buildString {
                appendLine("Execution of functions in child process failed with non-zero exit code = $exitCode: ")
                appendLine(processOutput)
            })
        }
    }

    companion object {
        private fun startWorkerProcess(
            testFunctionName: String,
            testFileName: String,
            goExecutableAbsolutePath: Path,
            gopathAbsolutePath: Path,
            workingDirectory: File,
        ): Process {
            val environment = modifyEnvironment(goExecutableAbsolutePath, gopathAbsolutePath)
            val command = listOf(testFileName, "--test.run", testFunctionName)
            return executeCommandByNewProcessOrFailWithoutWaiting(command, workingDirectory, environment)
        }

        private fun connectingToWorker(
            serverSocket: ServerSocket,
            process: Process,
            connectionTimeoutMillis: Long,
            endOfWorkerExecutionTimeout: Long,
        ): Socket {
            val workerSocket = try {
                serverSocket.soTimeout = connectionTimeoutMillis.toInt()
                serverSocket.accept()
            } catch (e: SocketTimeoutException) {
                val processHasExited = process.waitFor(endOfWorkerExecutionTimeout, TimeUnit.MILLISECONDS)
                if (processHasExited) {
                    throw GoWorkerFailedException("An error occurred while starting the worker.")
                } else {
                    process.destroy()
                }
                throw TimeoutException("Timeout exceeded: Worker not connected")
            }
            return workerSocket
        }

        fun createWorker(
            testFunctionName: String,
            testFilePath: String,
            goPackage: GoPackage,
            goExecutableAbsolutePath: Path,
            gopathAbsolutePath: Path,
            workingDirectory: File,
            serverSocket: ServerSocket,
            connectionTimeoutMillis: Long = 10000,
            endOfWorkerExecutionTimeout: Long = 5000,
        ): GoWorker {
            val workerProcess = startWorkerProcess(
                testFunctionName, testFilePath, goExecutableAbsolutePath, gopathAbsolutePath, workingDirectory
            )
            val workerSocket = connectingToWorker(
                serverSocket, workerProcess, connectionTimeoutMillis, endOfWorkerExecutionTimeout
            )
            return GoWorker(
                process = workerProcess,
                socket = workerSocket,
                testFunctionName = testFunctionName,
                testFilePath = testFilePath,
                goPackage = goPackage,
                goExecutableAbsolutePath = goExecutableAbsolutePath,
                gopathAbsolutePath = gopathAbsolutePath,
                workingDirectory = workingDirectory,
                serverSocket = serverSocket,
                readTimeoutMillis = 2 * endOfWorkerExecutionTimeout,
                connectionTimeoutMillis = connectionTimeoutMillis,
                endOfWorkerExecutionTimeoutMillis = endOfWorkerExecutionTimeout
            )
        }
    }
}