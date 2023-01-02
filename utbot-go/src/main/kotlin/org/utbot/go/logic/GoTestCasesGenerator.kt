package org.utbot.go.logic

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import org.utbot.go.GoEngine
import org.utbot.go.api.GoUtFile
import org.utbot.go.api.GoUtFunction
import org.utbot.go.api.GoUtFuzzedFunctionTestCase

object GoTestCasesGenerator {

    fun generateTestCasesForGoSourceFileFunctions(
        sourceFile: GoUtFile,
        functions: List<GoUtFunction>,
        goExecutableAbsolutePath: String,
        eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig,
    ): List<GoUtFuzzedFunctionTestCase> = runBlocking {
        return@runBlocking functions.flatMap { function ->
            val testCases = mutableListOf<GoUtFuzzedFunctionTestCase>()
            GoEngine(function, sourceFile, goExecutableAbsolutePath, eachExecutionTimeoutsMillisConfig).fuzzing()
                .catch {
                    error("Error in flow: ${it.message}")
                }
                .collect { (fuzzedFunction, executionResult) ->
                    testCases.add(GoUtFuzzedFunctionTestCase(fuzzedFunction, executionResult))
                }
            testCases
        }
    }
}