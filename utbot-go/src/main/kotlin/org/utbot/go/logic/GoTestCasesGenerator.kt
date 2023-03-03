package org.utbot.go.logic

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.utbot.go.GoEngine
import org.utbot.go.api.GoUtFile
import org.utbot.go.api.GoUtFunction
import org.utbot.go.api.GoUtFuzzedFunctionTestCase
import kotlin.system.measureTimeMillis

val logger = KotlinLogging.logger {}

object GoTestCasesGenerator {

    fun generateTestCasesForGoSourceFileFunctions(
        sourceFile: GoUtFile,
        functions: List<GoUtFunction>,
        intSize: Int,
        goExecutableAbsolutePath: String,
        eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig,
        timeoutExceededOrIsCanceled: (index: Int) -> Boolean
    ): List<GoUtFuzzedFunctionTestCase> = runBlocking {
        return@runBlocking functions.flatMapIndexed { index, function ->
            val testCases = mutableListOf<GoUtFuzzedFunctionTestCase>()
            if (timeoutExceededOrIsCanceled(index)) return@flatMapIndexed testCases
            val engine = GoEngine(
                function,
                sourceFile,
                intSize,
                goExecutableAbsolutePath,
                eachExecutionTimeoutsMillisConfig,
                { timeoutExceededOrIsCanceled(index) }
            )
            logger.info { "Fuzzing for function [${function.name}] - started" }
            val totalFuzzingTime = measureTimeMillis {
                engine.fuzzing().catch {
                    logger.error { "Error in flow: ${it.message}" }
                }.collect { (fuzzedFunction, executionResult) ->
                    testCases.add(GoUtFuzzedFunctionTestCase(fuzzedFunction, executionResult))
                }
            }
            logger.info { "Fuzzing for function [${function.name}] - completed in $totalFuzzingTime ms. Generated ${testCases.size} test cases" }
            testCases
        }
    }
}