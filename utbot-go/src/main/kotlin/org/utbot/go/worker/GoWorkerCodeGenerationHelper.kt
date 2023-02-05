package org.utbot.go.worker

import org.utbot.go.api.*
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.logic.EachExecutionTimeoutsMillisConfig
import org.utbot.go.simplecodegeneration.GoFileCodeBuilder
import java.io.File

internal object GoWorkerCodeGenerationHelper {

    const val workerTestFunctionName = "TestGoFileFuzzedFunctionsByUtGoWorker"

    private val alwaysRequiredImports = setOf(
        "io",
        "context",
        "encoding/json",
        "errors",
        "fmt",
        "log",
        "math",
        "net",
        "reflect",
        "strconv",
        "strings",
        "testing",
        "time",
        "unsafe"
    )

    fun createFileToExecute(
        sourceFile: GoUtFile,
        function: GoUtFunction,
        eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig,
        port: Int
    ): File {
        val fileToExecuteName = createFileToExecuteName(sourceFile)
        val sourceFileDir = File(sourceFile.absoluteDirectoryPath)
        val fileToExecute = sourceFileDir.resolve(fileToExecuteName)

        val fileToExecuteGoCode = generateWorkerTestFileGoCode(
            sourceFile, function, eachExecutionTimeoutsMillisConfig, port
        )
        fileToExecute.writeText(fileToExecuteGoCode)
        return fileToExecute
    }

    private fun createFileToExecuteName(sourceFile: GoUtFile): String {
        return "utbot_go_worker_${sourceFile.fileNameWithoutExtension}_test.go"
    }

    private fun generateWorkerTestFileGoCode(
        sourceFile: GoUtFile,
        function: GoUtFunction,
        eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig,
        port: Int
    ): String {
        fun GoTypeId.getAllStructTypes(): Set<GoStructTypeId> = when (this) {
            is GoStructTypeId -> fields.fold(setOf(this)) { acc: Set<GoStructTypeId>, field ->
                acc + (field.declaringType).getAllStructTypes()
            }

            is GoArrayTypeId -> elementTypeId!!.getAllStructTypes()
            else -> emptySet()
        }

        val structTypes =
            function.parameters.fold(emptySet()) { acc: Set<GoStructTypeId>, functionParameter: GoUtFunctionParameter ->
                acc + functionParameter.type.getAllStructTypes()
            }
        val imports = structTypes.fold(emptySet()) { acc: Set<String>, goStructTypeId ->
            if (goStructTypeId.packageName != sourceFile.packageName) acc + goStructTypeId.packagePath else acc
        }
        val fileCodeBuilder = GoFileCodeBuilder(sourceFile.packageName, alwaysRequiredImports + imports)

        val workerTestFunctionCode = generateWorkerTestFunctionCode(function, eachExecutionTimeoutsMillisConfig, port)
        val modifiedFunction = function.modifiedFunctionForCollectingTraces
        fileCodeBuilder.addTopLevelElements(
            GoCodeTemplates.getTopLevelHelperStructsAndFunctionsForWorker(
                structTypes, sourceFile.packageName
            ) + modifiedFunction + workerTestFunctionCode
        )

        return fileCodeBuilder.buildCodeString()
    }

    private fun generateWorkerTestFunctionCode(
        function: GoUtFunction, eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig, port: Int
    ): String {
        val timeoutMillis = eachExecutionTimeoutsMillisConfig[function]
        return """
            func $workerTestFunctionName(t *testing.T) {
            	con, err := net.Dial("tcp", ":$port")
            	__checkErrorAndExit__(err)

            	defer func(con net.Conn) {
            		err := con.Close()
            		if err != nil {
            			__checkErrorAndExit__(err)
            		}
            	}(con)

            	jsonDecoder := json.NewDecoder(con)
            	for {
            		rawValues, err := __parseJsonToRawValues__(jsonDecoder)
            		if err == io.EOF {
            			break
            		}
            		__checkErrorAndExit__(err)

            		parameters := __convertRawValuesToReflectValues__(rawValues)
            		function := reflect.ValueOf(${function.modifiedName})

            		executionResult := __executeFunction__($timeoutMillis*time.Millisecond, func() []__RawValue__ {
            			__traces__ = []int{}
            			return __wrapResultValuesForUtBotGoWorker__(function.Call(parameters))
            		})

            		jsonBytes, toJsonErr := json.MarshalIndent(executionResult, "", "  ")
            		__checkErrorAndExit__(toJsonErr)

            		_, err = con.Write([]byte(strconv.Itoa(len(jsonBytes)) + "\n"))
            		__checkErrorAndExit__(err)

            		_, err = con.Write(jsonBytes)
            		__checkErrorAndExit__(err)
            	}
            }
        """.trimIndent()
    }
}