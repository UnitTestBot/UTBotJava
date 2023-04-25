package org.utbot.go.logic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.go.GoEngine
import org.utbot.go.api.*
import org.utbot.go.imports.GoImportsResolver
import org.utbot.go.worker.GoWorker
import org.utbot.go.worker.GoWorkerCodeGenerationHelper
import org.utbot.go.worker.RawExecutionResult
import org.utbot.go.worker.convertRawExecutionResultToExecutionResult
import java.io.File
import java.net.ServerSocket
import java.nio.file.Path
import kotlin.system.measureTimeMillis

val logger = KotlinLogging.logger {}

object GoTestCasesGenerator {
    private const val NUM_OF_WORKERS = 4

    fun generateTestCasesForGoSourceFileFunctions(
        sourceFile: GoUtFile,
        functions: List<GoUtFunction>,
        intSize: Int,
        maxTraceLength: Int,
        goExecutableAbsolutePath: Path,
        gopathAbsolutePath: Path,
        eachExecutionTimeoutMillis: Long,
        timeoutExceededOrIsCanceled: (index: Int) -> Boolean = { false },
    ): List<GoUtFuzzedFunctionTestCase> = ServerSocket(0).use { serverSocket ->
        val allRawExecutionResults = mutableListOf<Pair<GoUtFuzzedFunction, RawExecutionResult>>()
        var fileToExecute: File? = null
        var fileWithModifiedFunctions: File? = null
        try {
            // creating files for workers
            val types = functions.flatMap { it.parameters }.map { it.type }
            val imports = GoImportsResolver.resolveImportsBasedOnTypes(
                types, sourceFile.sourcePackage, GoWorkerCodeGenerationHelper.alwaysRequiredImports
            )
            val aliases = imports.filter { it.alias != null }.associate { it.goPackage to it.alias }
            fileToExecute = GoWorkerCodeGenerationHelper.createFileToExecute(
                sourceFile, functions, eachExecutionTimeoutMillis, serverSocket.localPort, maxTraceLength, imports
            )
            fileWithModifiedFunctions = GoWorkerCodeGenerationHelper.createFileWithModifiedFunctions(
                sourceFile, functions
            )

            // starting worker processes
            val workers = runBlocking {
                val testFunctionName = GoWorkerCodeGenerationHelper.workerTestFunctionName
                val goPackage = sourceFile.sourcePackage
                val sourceFileDir = File(sourceFile.absoluteDirectoryPath)
                return@runBlocking (1..NUM_OF_WORKERS).map {
                    async(Dispatchers.IO) {
                        GoWorker.createWorker(
                            testFunctionName,
                            goPackage,
                            goExecutableAbsolutePath,
                            gopathAbsolutePath,
                            sourceFileDir,
                            serverSocket
                        )
                    }
                }.awaitAll()
            }

            try {
                functions.forEachIndexed { index, function ->
                    if (timeoutExceededOrIsCanceled(index)) return@forEachIndexed
                    val rawExecutionResults = mutableListOf<Pair<GoUtFuzzedFunction, RawExecutionResult>>()
                    val engine = GoEngine(
                        workers = workers, functionUnderTest = function, aliases = aliases, intSize = intSize
                    ) { timeoutExceededOrIsCanceled(index) }
                    logger.info { "Fuzzing for function [${function.name}] - started" }
                    val totalFuzzingTime = runBlocking {
                        measureTimeMillis {
                            engine.fuzzing().catch {
                                logger.error { "Error in flow: ${it.message}" }
                            }.collect { (fuzzedFunction, executionResult) ->
                                rawExecutionResults.add(fuzzedFunction to executionResult)
                            }
                        }
                    }
                    logger.debug { "Number of function executions - ${engine.numberOfFunctionExecutions} (${engine.numberOfFunctionExecutions.get() / (totalFuzzingTime / 1000)}/sec)" }
                    logger.info { "Fuzzing for function [${function.name}] - completed in $totalFuzzingTime ms. Generated ${rawExecutionResults.size} test cases" }
                    allRawExecutionResults += rawExecutionResults
                }
                runBlocking {
                    workers.map { async(Dispatchers.IO) { it.close() } }.awaitAll()
                }
            } catch (e: TimeoutException) {
                logger.error { e.message }
            } catch (e: RuntimeException) {
                logger.error { e.message }
            }
        } finally {
            fileToExecute?.delete()
            fileWithModifiedFunctions?.delete()
        }
        return allRawExecutionResults.map { (fuzzedFunction, rawExecutionResult) ->
            val executionResult: GoUtExecutionResult = convertRawExecutionResultToExecutionResult(
                rawExecutionResult, fuzzedFunction.function.resultTypes, intSize, eachExecutionTimeoutMillis
            )
            GoUtFuzzedFunctionTestCase(fuzzedFunction, executionResult)
        }
    }
}
