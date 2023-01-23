package org.utbot.go.executor

import org.utbot.go.api.GoUtFile
import org.utbot.go.api.GoUtFuzzedFunction
import org.utbot.go.logic.EachExecutionTimeoutsMillisConfig
import org.utbot.go.simplecodegeneration.GoFileCodeBuilder
import org.utbot.go.simplecodegeneration.generateFuzzedFunctionCall
import org.utbot.go.util.goRequiredImports

internal object GoFuzzedFunctionExecutorCodeGenerationHelper {

    private val alwaysRequiredImports =
        setOf("context", "encoding/json", "errors", "fmt", "math", "os", "reflect", "strconv", "strings", "testing", "time", "log")

    fun generateExecutorTestFileGoCode(
        sourceFile: GoUtFile,
        fuzzedFunction: GoUtFuzzedFunction,
        eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig,
        executorTestFunctionName: String,
        rawExecutionResultsFileName: String,
    ): String {
        val additionalImports = mutableSetOf<String>()
        fuzzedFunction.fuzzedParametersValues.forEach {
            additionalImports += it.goRequiredImports
        }

        val fileCodeBuilder = GoFileCodeBuilder(sourceFile.packageName, alwaysRequiredImports + additionalImports)

        val executorTestFunctionCode = generateExecutorTestFunctionCode(
            fuzzedFunction, eachExecutionTimeoutsMillisConfig, executorTestFunctionName, rawExecutionResultsFileName
        )
        val modifiedFunction = fuzzedFunction.function.modifiedFunctionForCollectingTraces
        fileCodeBuilder.addTopLevelElements(
            GoCodeTemplates.topLevelHelperStructsAndFunctionsForExecutor + modifiedFunction + listOf(
                executorTestFunctionCode
            )
        )

        return fileCodeBuilder.buildCodeString()
    }

    private fun generateExecutorTestFunctionCode(
        fuzzedFunction: GoUtFuzzedFunction,
        eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig,
        executorTestFunctionName: String,
        rawExecutionResultsFileName: String,
    ): String {
        val function = fuzzedFunction.function
        val fuzzedFunctionCall =
            generateFuzzedFunctionCall(function.modifiedName, fuzzedFunction.fuzzedParametersValues)
        val timeoutMillis = eachExecutionTimeoutsMillisConfig[function]

        return if (function.resultTypes.isEmpty()) {
            """
                func $executorTestFunctionName(t *testing.T) {
                    executionResult := __executeFunction__($timeoutMillis*time.Millisecond, func() []__RawValue__ {
                        __traces__ = []int{}
                        $fuzzedFunctionCall
                        return []__RawValue__{}
                    })
                    
                    jsonBytes, toJsonErr := json.MarshalIndent(executionResult, "", "  ")
                    __checkErrorAndExit__(toJsonErr)
    
                    const resultsFilePath = "$rawExecutionResultsFileName"
                    writeErr := os.WriteFile(resultsFilePath, jsonBytes, os.ModePerm)
                    __checkErrorAndExit__(writeErr)
                }
            """.trimIndent()
        } else {
            """
                func $executorTestFunctionName(t *testing.T) {
                    executionResult := __executeFunction__($timeoutMillis*time.Millisecond, func() []__RawValue__ {
                        __traces__ = []int{}
                        return __wrapResultValuesForUtBotGoExecutor__($fuzzedFunctionCall)
                    })
                    
                    jsonBytes, toJsonErr := json.MarshalIndent(executionResult, "", "  ")
                    __checkErrorAndExit__(toJsonErr)
    
                    const resultsFilePath = "$rawExecutionResultsFileName"
                    writeErr := os.WriteFile(resultsFilePath, jsonBytes, os.ModePerm)
                    __checkErrorAndExit__(writeErr)
                }
            """.trimIndent()
        }
    }
}