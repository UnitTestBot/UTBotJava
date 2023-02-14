package org.utbot.go.worker

import org.utbot.go.api.*
import org.utbot.go.framework.api.go.GoImport
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.logic.EachExecutionTimeoutsMillisConfig
import org.utbot.go.simplecodegeneration.GoFileCodeBuilder
import java.io.File

internal object GoWorkerCodeGenerationHelper {

    const val workerTestFunctionName = "TestGoFileFuzzedFunctionsByUtGoWorker"

    val alwaysRequiredImports = setOf(
        GoPackage("io", "io"),
        GoPackage("context", "context"),
        GoPackage("json", "encoding/json"),
        GoPackage("errors", "errors"),
        GoPackage("fmt", "fmt"),
        GoPackage("log", "log"),
        GoPackage("math", "math"),
        GoPackage("net", "net"),
        GoPackage("reflect", "reflect"),
        GoPackage("strconv", "strconv"),
        GoPackage("strings", "strings"),
        GoPackage("testing", "testing"),
        GoPackage("time", "time"),
        GoPackage("unsafe", "unsafe")
    ).map { GoImport(it, "") }.toSet()

    fun createFileToExecute(
        sourceFile: GoUtFile,
        function: GoUtFunction,
        eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig,
        port: Int,
        aliases: Map<GoPackage, String>
    ): File {
        val fileToExecuteName = createFileToExecuteName(sourceFile)
        val sourceFileDir = File(sourceFile.absoluteDirectoryPath)
        val fileToExecute = sourceFileDir.resolve(fileToExecuteName)

        val fileToExecuteGoCode =
            generateWorkerTestFileGoCode(function, eachExecutionTimeoutsMillisConfig, port, aliases)
        fileToExecute.writeText(fileToExecuteGoCode)
        return fileToExecute
    }

    fun createFileWithModifiedFunction(
        function: GoUtFunction,
        sourceFileDir: File
    ): File {
        val fileWithModifiedFunctionName = createFileWithModifiedFunctionName()
        val fileWithModifiedFunction = sourceFileDir.resolve(fileWithModifiedFunctionName)

        val fileWithModifiedFunctionGoCode = generateFileWithModifiedFunctionGoCode(function)
        fileWithModifiedFunction.writeText(fileWithModifiedFunctionGoCode)
        return fileWithModifiedFunction
    }

    private fun createFileToExecuteName(sourceFile: GoUtFile): String {
        return "utbot_go_worker_${sourceFile.fileNameWithoutExtension}_test.go"
    }

    private fun createFileWithModifiedFunctionName(): String {
        return "utbot_go_modified_function.go"
    }

    private fun generateWorkerTestFileGoCode(
        function: GoUtFunction,
        eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig,
        port: Int,
        aliases: Map<GoPackage, String>
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

        val fileCodeBuilder = GoFileCodeBuilder(
            function.sourceFile.sourcePackage, alwaysRequiredImports + structTypes.map {
                GoImport(it.sourcePackage, aliases[it.sourcePackage] ?: "")
            }
        )

        val workerTestFunctionCode = generateWorkerTestFunctionCode(function, eachExecutionTimeoutsMillisConfig, port)
        fileCodeBuilder.addTopLevelElements(
            GoCodeTemplates.getTopLevelHelperStructsAndFunctionsForWorker(
                structTypes, function.sourceFile.sourcePackage, aliases
            ) + workerTestFunctionCode
        )

        return fileCodeBuilder.buildCodeString()
    }

    private fun generateFileWithModifiedFunctionGoCode(function: GoUtFunction): String {
        val fileCodeBuilder = GoFileCodeBuilder(function.sourcePackage, function.requiredImports.toSet())
        fileCodeBuilder.addTopLevelElements(listOf(GoCodeTemplates.traces) + function.modifiedFunctionForCollectingTraces)
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