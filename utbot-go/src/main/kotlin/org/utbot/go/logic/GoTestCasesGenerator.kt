package org.utbot.go.logic

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import mu.KotlinLogging
import org.utbot.common.isWindows
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.go.GoEngine
import org.utbot.go.api.*
import org.utbot.go.imports.GoImportsResolver
import org.utbot.go.util.executeCommandByNewProcessOrFail
import org.utbot.go.util.modifyEnvironment
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
        absolutePathToInstrumentedPackage: String,
        absolutePathToInstrumentedModule: String,
        needToCoverLines: Map<String, List<String>>,
        intSize: Int,
        goExecutableAbsolutePath: Path,
        gopathAbsolutePath: Path,
        eachExecutionTimeoutMillis: Long,
        timeoutExceededOrIsCanceled: (index: Int) -> Boolean = { false },
    ): List<GoUtFuzzedFunctionTestCase> = ServerSocket(0).use { serverSocket ->
        val allRawExecutionResults = mutableListOf<Pair<GoUtFuzzedFunction, RawExecutionResult>>()
        var testFile: File? = null
        try {
            // creating files for workers
            val types = functions.flatMap { it.parameters }.map { it.type }
            val imports = GoImportsResolver.resolveImportsBasedOnTypes(
                types, sourceFile.sourcePackage, GoWorkerCodeGenerationHelper.alwaysRequiredImports
            )
            val aliases = imports.filter { it.alias != null }.associate { it.goPackage to it.alias }
            GoWorkerCodeGenerationHelper.createFileToExecute(
                sourceFile,
                functions,
                absolutePathToInstrumentedPackage,
                eachExecutionTimeoutMillis,
                serverSocket.localPort,
                imports
            )

            GoWorkerCodeGenerationHelper.createFileWithCoverTab(
                sourceFile, absolutePathToInstrumentedPackage
            )

            // adding missing and removing unused modules in instrumented package
            val environment = modifyEnvironment(goExecutableAbsolutePath, gopathAbsolutePath)
            val modCommand = listOf(
                goExecutableAbsolutePath.toString(), "mod", "tidy"
            )
            logger.info { "Adding missing and removing unused modules in instrumented package [$absolutePathToInstrumentedPackage] - started" }
            val goModTime = measureTimeMillis {
                executeCommandByNewProcessOrFail(
                    modCommand,
                    File(absolutePathToInstrumentedPackage),
                    "adding missing and removing unused modules in instrumented package",
                    environment
                )
            }
            logger.info { "Adding missing and removing unused modules in instrumented package [$absolutePathToInstrumentedPackage] - completed in [$goModTime] (ms)" }

            // compiling the test binary
            testFile = run {
                if (isWindows) {
                    Path.of(sourceFile.absoluteDirectoryPath, "utbot_go_test.exe")
                } else {
                    Path.of(sourceFile.absoluteDirectoryPath, "utbot_go_test")
                }
            }.toFile()
            val buildCommand = listOf(
                goExecutableAbsolutePath.toString(), "test", "-c", "-o", testFile.absolutePath
            )
            logger.info { "Compiling the test binary - started" }
            val compilingTestBinaryTime = measureTimeMillis {
                executeCommandByNewProcessOrFail(
                    buildCommand,
                    File(absolutePathToInstrumentedPackage),
                    "compiling the test binary to the $testFile file",
                    environment
                )
            }
            logger.info { "Compiling the test binary - completed in [$compilingTestBinaryTime] (ms)" }

            // starting worker processes
            logger.debug { "Creation of workers - started" }
            val creationOfWorkersStartTime = System.currentTimeMillis()
            val workers = runBlocking {
                val testFunctionName = GoWorkerCodeGenerationHelper.workerTestFunctionName
                val goPackage = sourceFile.sourcePackage
                val sourceFileDir = File(sourceFile.absoluteDirectoryPath)
                return@runBlocking (1..NUM_OF_WORKERS).map {
                    async(Dispatchers.IO) {
                        GoWorker.createWorker(
                            testFunctionName,
                            testFile.absolutePath,
                            goPackage,
                            goExecutableAbsolutePath,
                            gopathAbsolutePath,
                            sourceFileDir,
                            serverSocket
                        )
                    }
                }.awaitAll()
            }
            logger.debug { "Creation of workers - completed in [${System.currentTimeMillis() - creationOfWorkersStartTime}] (ms)" }

            // fuzzing
            functions.forEachIndexed { index, function ->
                if (timeoutExceededOrIsCanceled(index)) return@forEachIndexed
                val rawExecutionResults = mutableListOf<Pair<GoUtFuzzedFunction, RawExecutionResult>>()
                val engine = GoEngine(
                    workers = workers,
                    functionUnderTest = function,
                    needToCoverLines = needToCoverLines[function.name]!!,
                    aliases = aliases,
                    intSize = intSize
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
                val numberOfExecutionsPerSecond = if (totalFuzzingTime / 1000 != 0L) {
                    engine.numberOfFunctionExecutions.get() / (totalFuzzingTime / 1000)
                } else {
                    ">${engine.numberOfFunctionExecutions}"
                }.toString()
                logger.debug { "Number of function executions - [${engine.numberOfFunctionExecutions}] ($numberOfExecutionsPerSecond/sec)" }
                logger.info { "Fuzzing for function [${function.name}] - completed in [$totalFuzzingTime] (ms). Generated [${rawExecutionResults.size}] test cases" }
                allRawExecutionResults += rawExecutionResults
            }
            runBlocking {
                workers.map { launch(Dispatchers.IO) { it.close() } }.joinAll()
            }
        } catch (e: TimeoutException) {
            logger.error { e.message }
        } catch (e: RuntimeException) {
            logger.error { e.message }
        } finally {
            // delete test file and directory with instrumented packages
            testFile?.delete()
            File(absolutePathToInstrumentedModule).delete()
        }
        return allRawExecutionResults.map { (fuzzedFunction, rawExecutionResult) ->
            val executionResult: GoUtExecutionResult = convertRawExecutionResultToExecutionResult(
                rawExecutionResult, fuzzedFunction.function.resultTypes, intSize, eachExecutionTimeoutMillis
            )
            GoUtFuzzedFunctionTestCase(fuzzedFunction, executionResult)
        }
    }
}
