package org.utbot.go.logic

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.go.GoEngine
import org.utbot.go.api.*
import org.utbot.go.imports.GoImportsResolver
import org.utbot.go.util.executeCommandByNewProcessOrFailWithoutWaiting
import org.utbot.go.util.modifyEnvironment
import org.utbot.go.worker.*
import java.io.File
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

val logger = KotlinLogging.logger {}

object GoTestCasesGenerator {

    fun generateTestCasesForGoSourceFileFunctions(
        sourceFile: GoUtFile,
        functions: List<GoUtFunction>,
        intSize: Int,
        maxTraceLength: Int,
        goExecutableAbsolutePath: Path,
        gopathAbsolutePath: Path,
        eachExecutionTimeoutMillis: Long,
        connectionTimeoutMillis: Long = 10000,
        endOfWorkerExecutionTimeout: Long = 5000,
        timeoutExceededOrIsCanceled: (index: Int) -> Boolean = { false },
    ): List<GoUtFuzzedFunctionTestCase> = runBlocking {
        ServerSocket(0).use { serverSocket ->
            val allRawExecutionResults = mutableListOf<Pair<GoUtFuzzedFunction, RawExecutionResult>>()
            var fileToExecute: File? = null
            var fileWithModifiedFunctions: File? = null
            try {
                // creating files for worker
                val types = functions.flatMap { it.parameters }.map { it.type }
                val imports = GoImportsResolver.resolveImportsBasedOnTypes(
                    types,
                    sourceFile.sourcePackage,
                    GoWorkerCodeGenerationHelper.alwaysRequiredImports
                )
                val aliases = imports.filter { it.alias != null }.associate { it.goPackage to it.alias }
                fileToExecute = GoWorkerCodeGenerationHelper.createFileToExecute(
                    sourceFile,
                    functions,
                    eachExecutionTimeoutMillis,
                    serverSocket.localPort,
                    maxTraceLength,
                    imports
                )
                fileWithModifiedFunctions = GoWorkerCodeGenerationHelper.createFileWithModifiedFunctions(
                    sourceFile, functions
                )

                // starting worker process
                val testFunctionName = GoWorkerCodeGenerationHelper.workerTestFunctionName
                val command = listOf(
                    goExecutableAbsolutePath.toString(), "test", "-run", testFunctionName, "-timeout", "0"
                )
                val sourceFileDir = File(sourceFile.absoluteDirectoryPath)
                val processStartTime = System.currentTimeMillis()
                val environment = modifyEnvironment(goExecutableAbsolutePath, gopathAbsolutePath)
                val process = executeCommandByNewProcessOrFailWithoutWaiting(command, sourceFileDir, environment)

                try {
                    // connecting to worker
                    logger.debug { "Trying to connect to worker" }
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
                    val worker = GoWorker(workerSocket, sourceFile.sourcePackage)
                    logger.debug { "Worker connected - completed in ${System.currentTimeMillis() - processStartTime} ms" }
                    functions.forEachIndexed { index, function ->
                        if (timeoutExceededOrIsCanceled(index)) return@forEachIndexed
                        val rawExecutionResults = mutableListOf<Pair<GoUtFuzzedFunction, RawExecutionResult>>()
                        val engine = GoEngine(worker, function, aliases, intSize) { timeoutExceededOrIsCanceled(index) }
                        logger.info { "Fuzzing for function [${function.name}] - started" }
                        val totalFuzzingTime = measureTimeMillis {
                            engine.fuzzing().catch {
                                logger.error { "Error in flow: ${it.message}" }
                            }.collect { (fuzzedFunction, executionResult) ->
                                rawExecutionResults.add(fuzzedFunction to executionResult)
                            }
                        }
                        logger.debug { "Number of function executions - ${engine.numberOfFunctionExecutions}" }
                        logger.info { "Fuzzing for function [${function.name}] - completed in $totalFuzzingTime ms. Generated ${rawExecutionResults.size} test cases" }
                        allRawExecutionResults += rawExecutionResults
                    }
                    workerSocket.close()
                    val processHasExited = process.waitFor(endOfWorkerExecutionTimeout, TimeUnit.MILLISECONDS)
                    if (!processHasExited) {
                        process.destroy()
                        val processOutput = InputStreamReader(process.inputStream).readText()
                        throw TimeoutException(
                            buildString {
                                appendLine("Timeout exceeded: Worker didn't finish. Process output: ")
                                appendLine(processOutput)
                            }
                        )
                    }
                    val exitCode = process.exitValue()
                    if (exitCode != 0) {
                        val processOutput = InputStreamReader(process.inputStream).readText()
                        throw RuntimeException(
                            buildString {
                                appendLine("Execution of functions from $sourceFile in child process failed with non-zero exit code = $exitCode: ")
                                appendLine(processOutput)
                            }
                        )
                    }
                } catch (e: TimeoutException) {
                    logger.error { e.message }
                } catch (e: RuntimeException) {
                    logger.error { e.message }
                } catch (e: SocketException) {
                    val processHasExited = process.waitFor(endOfWorkerExecutionTimeout, TimeUnit.MILLISECONDS)
                    if (!processHasExited) {
                        process.destroy()
                        val processOutput = InputStreamReader(process.inputStream).readText()
                        logger.error {
                            buildString {
                                appendLine("Timeout exceeded: Worker didn't finish. Process output: ")
                                appendLine(processOutput)
                            }
                        }
                    }
                    val exitCode = process.exitValue()
                    if (exitCode != 0) {
                        val processOutput = InputStreamReader(process.inputStream).readText()
                        logger.error {
                            buildString {
                                appendLine("Execution of functions from $sourceFile in child process failed with non-zero exit code = $exitCode: ")
                                appendLine(processOutput)
                            }
                        }
                    }

                }
            } finally {
                fileToExecute?.delete()
                fileWithModifiedFunctions?.delete()
            }
            return@runBlocking allRawExecutionResults.map { (fuzzedFunction, rawExecutionResult) ->
                val executionResult: GoUtExecutionResult = convertRawExecutionResultToExecutionResult(
                    rawExecutionResult,
                    fuzzedFunction.function.resultTypes,
                    intSize,
                    eachExecutionTimeoutMillis
                )
                GoUtFuzzedFunctionTestCase(fuzzedFunction, executionResult)
            }
        }
    }
}
