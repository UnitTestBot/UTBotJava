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
        goExecutableAbsolutePath: String,
        eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig,
    ): List<GoUtFuzzedFunctionTestCase> = runBlocking {
        return@runBlocking functions.flatMap { function ->
            val testCases = mutableListOf<GoUtFuzzedFunctionTestCase>()
            logger.info { "Fuzzing for function ${function.name} - started" }
            val totalFuzzingTime = measureTimeMillis {
                GoEngine(function, sourceFile, goExecutableAbsolutePath, eachExecutionTimeoutsMillisConfig).fuzzing()
                    .catch {
                        error("Error in flow: ${it.message}")
                    }
                    .collect { (fuzzedFunction, executionResult) ->
                        testCases.add(GoUtFuzzedFunctionTestCase(fuzzedFunction, executionResult))
                    }
            }
            logger.info { "Fuzzing for function ${function.name} - completed in $totalFuzzingTime ms" }
            testCases
        }
    }
}