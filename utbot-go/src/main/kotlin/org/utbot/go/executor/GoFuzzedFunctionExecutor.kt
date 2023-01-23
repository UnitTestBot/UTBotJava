package org.utbot.go.executor

import mu.KotlinLogging
import org.utbot.go.api.GoUtExecutionResult
import org.utbot.go.api.GoUtFile
import org.utbot.go.api.GoUtFuzzedFunction
import org.utbot.go.logic.EachExecutionTimeoutsMillisConfig
import org.utbot.go.util.executeCommandByNewProcessOrFail
import org.utbot.go.util.parseFromJsonOrFail
import java.io.File
import kotlin.system.measureTimeMillis

val logger = KotlinLogging.logger {}

object GoFuzzedFunctionsExecutor {

    fun executeGoSourceFileFuzzedFunction(
        sourceFile: GoUtFile,
        fuzzedFunction: GoUtFuzzedFunction,
        goExecutableAbsolutePath: String,
        eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig
    ): GoUtExecutionResult {
        val fileToExecuteName = createFileToExecuteName(sourceFile)
        val rawExecutionResultsFileName = createRawExecutionResultsFileName(sourceFile)

        val sourceFileDir = File(sourceFile.absoluteDirectoryPath)
        val fileToExecute = sourceFileDir.resolve(fileToExecuteName)
        val rawExecutionResultFile = sourceFileDir.resolve(rawExecutionResultsFileName)

        val executorTestFunctionName = createExecutorTestFunctionName()
        val runGeneratedGoExecutorTestCommand = listOf(
            goExecutableAbsolutePath, "test", "-run", executorTestFunctionName
        )

        try {
            val fileToExecuteGoCode = GoFuzzedFunctionExecutorCodeGenerationHelper.generateExecutorTestFileGoCode(
                sourceFile,
                fuzzedFunction,
                eachExecutionTimeoutsMillisConfig,
                executorTestFunctionName,
                rawExecutionResultsFileName
            )
            fileToExecute.writeText(fileToExecuteGoCode)

            logger.debug { "Function execution [${fuzzedFunction.function.name}] - started" }
            val totalExecutionTime = measureTimeMillis {
                executeCommandByNewProcessOrFail(
                    runGeneratedGoExecutorTestCommand,
                    sourceFileDir,
                    "function [${fuzzedFunction.function.name}] from $sourceFile",
                    StringBuilder().append("Try reducing the timeout for each function execution, ")
                        .append("select fewer functions for test generation at the same time, ")
                        .append("or handle corner cases in the source code. ")
                        .append("Perhaps some functions are too resource-intensive.").toString()
                )
            }
            logger.debug { "Function execution [${fuzzedFunction.function.name}] - completed in $totalExecutionTime ms" }

            logger.debug { "Parsing execution result for function ${fuzzedFunction.function.name} - started" }
            var rawExecutionResult: RawExecutionResult
            val totalParsingTime = measureTimeMillis {
                rawExecutionResult = parseFromJsonOrFail(rawExecutionResultFile)
            }
            logger.debug { "Parsing execution result for function ${fuzzedFunction.function.name} - completed in $totalParsingTime ms" }

            return convertRawExecutionResultToExecutionResult(
                fuzzedFunction.function.getPackageName(),
                rawExecutionResult,
                fuzzedFunction.function.resultTypes,
                eachExecutionTimeoutsMillisConfig[fuzzedFunction.function],
            )
        } finally {
            fileToExecute.delete()
            rawExecutionResultFile.delete()
        }
    }

    private fun createFileToExecuteName(sourceFile: GoUtFile): String {
        return "utbot_go_executor_${sourceFile.fileNameWithoutExtension}_test.go"
    }

    private fun createRawExecutionResultsFileName(sourceFile: GoUtFile): String {
        return "utbot_go_executor_${sourceFile.fileNameWithoutExtension}_test_results.json"
    }

    private fun createExecutorTestFunctionName(): String {
        return "TestGoFileFuzzedFunctionsByUtGoExecutor"
    }
}