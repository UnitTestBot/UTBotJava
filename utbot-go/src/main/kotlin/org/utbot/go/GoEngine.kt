package org.utbot.go

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.fuzzing.BaseFeedback
import org.utbot.fuzzing.Control
import org.utbot.fuzzing.utils.Trie
import org.utbot.go.api.*
import org.utbot.go.executor.GoFuzzedFunctionsExecutor
import org.utbot.go.executor.GoWorker
import org.utbot.go.executor.GoWorkerCodeGenerationHelper
import org.utbot.go.executor.convertRawExecutionResultToExecutionResult
import org.utbot.go.logic.EachExecutionTimeoutsMillisConfig
import org.utbot.go.util.executeCommandByNewProcessOrFailWithoutWaiting
import java.io.File
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.SocketTimeoutException

val logger = KotlinLogging.logger {}

class GoEngine(
    private val methodUnderTest: GoUtFunction,
    private val sourceFile: GoUtFile,
    private val goExecutableAbsolutePath: String,
    private val eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig,
    private val timeoutExceededOrIsCanceled: () -> Boolean,
    private val timeout: Int = 5000
) {

    fun fuzzing(): Flow<Pair<GoUtFuzzedFunction, GoUtExecutionResult>> = flow {
        var attempts = 0
        val attemptsLimit = Int.MAX_VALUE
        runGoFuzzing(methodUnderTest) { description, values ->
            if (timeoutExceededOrIsCanceled()) {
                return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
            }
            val fuzzedFunction = GoUtFuzzedFunction(methodUnderTest, values)
            val executionResult = GoFuzzedFunctionsExecutor.executeGoSourceFileFuzzedFunction(
                sourceFile,
                fuzzedFunction,
                goExecutableAbsolutePath,
                eachExecutionTimeoutsMillisConfig
            )
            if (executionResult.trace.isEmpty()) {
                logger.error { "Coverage is empty for [${methodUnderTest.name}] with ${values.map { it.model }}" }
                if (executionResult is GoUtPanicFailure) {
                    logger.error { "Execution completed with panic: ${executionResult.panicValue}" }
                }
                return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.PASS)
            }
            val trieNode = description.tracer.add(executionResult.trace.map { GoInstruction(it) })
            if (trieNode.count > 1) {
                if (++attempts >= attemptsLimit) {
                    return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                }
                return@runGoFuzzing BaseFeedback(result = trieNode, control = Control.CONTINUE)
            }
            emit(fuzzedFunction to executionResult)
            BaseFeedback(result = trieNode, control = Control.CONTINUE)
        }
    }

    fun fastFuzzing(): Flow<Pair<GoUtFuzzedFunction, GoUtExecutionResult>> = flow {
        var attempts = 0
        val attemptsLimit = Int.MAX_VALUE
        ServerSocket(0).use { serverSocket ->
            var fileToExecute: File? = null
            try {
                fileToExecute = GoWorkerCodeGenerationHelper.createFileToExecute(
                    sourceFile,
                    methodUnderTest,
                    eachExecutionTimeoutsMillisConfig,
                    serverSocket.localPort
                )
                val testFunctionName = GoWorkerCodeGenerationHelper.workerTestFunctionName
                val command = listOf(
                    goExecutableAbsolutePath, "test", "-run", testFunctionName
                )
                val sourceFileDir = File(sourceFile.absoluteDirectoryPath)
                val process = executeCommandByNewProcessOrFailWithoutWaiting(command, sourceFileDir)
                val workerSocket = try {
                    serverSocket.soTimeout = timeout
                    serverSocket.accept()
                } catch (e: SocketTimeoutException) {
                    process.destroy()
                    val exitCode = process.exitValue()
                    if (exitCode != 0) {
                        val processOutput = InputStreamReader(process.inputStream).readText()
                        throw RuntimeException(
                            StringBuilder()
                                .append("Execution of ${"function [${methodUnderTest.name}] from $sourceFile"} in child process failed with non-zero exit code = $exitCode: ")
                                .append("\n$processOutput")
                                .toString()
                        )
                    }
                    throw TimeoutException("Timeout exceeded: Worker not connected")
                }
                val worker = GoWorker(workerSocket)
                workerSocket.soTimeout = timeout
                if (methodUnderTest.parameters.isEmpty()) {
                    worker.sendFuzzedParametersValues(listOf())
                    val rawExecutionResult = worker.receiveRawExecutionResult()
                    val executionResult = convertRawExecutionResultToExecutionResult(
                        methodUnderTest.getPackageName(),
                        rawExecutionResult,
                        methodUnderTest.resultTypes,
                        eachExecutionTimeoutsMillisConfig[methodUnderTest],
                    )
                    val fuzzedFunction = GoUtFuzzedFunction(methodUnderTest, listOf())
                    emit(fuzzedFunction to executionResult)
                } else {
                    runGoFuzzing(methodUnderTest) { description, values ->
                        if (timeoutExceededOrIsCanceled()) {
                            return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                        }
                        val fuzzedFunction = GoUtFuzzedFunction(methodUnderTest, values)
                        worker.sendFuzzedParametersValues(values)
                        val rawExecutionResult = worker.receiveRawExecutionResult()
                        val executionResult = convertRawExecutionResultToExecutionResult(
                            methodUnderTest.getPackageName(),
                            rawExecutionResult,
                            methodUnderTest.resultTypes,
                            eachExecutionTimeoutsMillisConfig[methodUnderTest],
                        )
                        if (executionResult.trace.isEmpty()) {
                            logger.error { "Coverage is empty for [${methodUnderTest.name}] with ${values.map { it.model }}" }
                            if (executionResult is GoUtPanicFailure) {
                                logger.error { "Execution completed with panic: ${executionResult.panicValue}" }
                            }
                            return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.PASS)
                        }
                        val trieNode = description.tracer.add(executionResult.trace.map { GoInstruction(it) })
                        if (trieNode.count > 1) {
                            if (++attempts >= attemptsLimit) {
                                return@runGoFuzzing BaseFeedback(result = Trie.emptyNode(), control = Control.STOP)
                            }
                            return@runGoFuzzing BaseFeedback(result = trieNode, control = Control.CONTINUE)
                        }
                        emit(fuzzedFunction to executionResult)
                        BaseFeedback(result = trieNode, control = Control.CONTINUE)
                    }
                    workerSocket.close()
                    process.waitFor()
                    val exitCode = process.exitValue()
                    if (exitCode != 0) {
                        val processOutput = InputStreamReader(process.inputStream).readText()
                        throw RuntimeException(
                            StringBuilder()
                                .append("Execution of ${"function [${methodUnderTest.name}] from $sourceFile"} in child process failed with non-zero exit code = $exitCode: ")
                                .append("\n$processOutput")
                                .toString()
                        )
                    }
                }
            } finally {
                fileToExecute?.delete()
            }
        }
    }
}