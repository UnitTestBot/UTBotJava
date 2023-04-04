package org.utbot.go.worker

import org.utbot.go.api.GoUtFile
import org.utbot.go.api.GoUtFunction
import org.utbot.go.api.util.getAllVisibleNamedTypes
import org.utbot.go.framework.api.go.GoImport
import org.utbot.go.framework.api.go.GoPackage
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
    ).map { GoImport(it) }.toSet()

    fun createFileToExecute(
        sourceFile: GoUtFile,
        functions: List<GoUtFunction>,
        eachExecutionTimeoutMillis: Long,
        port: Int,
        maxTraceLength: Int,
        imports: Set<GoImport>
    ): File {
        val fileToExecuteName = createFileToExecuteName(sourceFile)
        val sourceFileDir = File(sourceFile.absoluteDirectoryPath)
        val fileToExecute = sourceFileDir.resolve(fileToExecuteName)

        val fileToExecuteGoCode =
            generateWorkerTestFileGoCode(
                sourceFile,
                functions,
                eachExecutionTimeoutMillis,
                port,
                maxTraceLength,
                imports
            )
        fileToExecute.writeText(fileToExecuteGoCode)
        return fileToExecute
    }

    fun createFileWithModifiedFunctions(
        sourceFile: GoUtFile,
        functions: List<GoUtFunction>
    ): File {
        val fileWithModifiedFunctionsName = createFileWithModifiedFunctionsName()
        val sourceFileDir = File(sourceFile.absoluteDirectoryPath)
        val fileWithModifiedFunctions = sourceFileDir.resolve(fileWithModifiedFunctionsName)

        val fileWithModifiedFunctionsGoCode = generateFileWithModifiedFunctionsGoCode(sourceFile, functions)
        fileWithModifiedFunctions.writeText(fileWithModifiedFunctionsGoCode)
        return fileWithModifiedFunctions
    }

    private fun createFileToExecuteName(sourceFile: GoUtFile): String {
        return "utbot_go_worker_${sourceFile.fileNameWithoutExtension}_test.go"
    }

    private fun createFileWithModifiedFunctionsName(): String {
        return "utbot_go_modified_functions.go"
    }

    private fun generateWorkerTestFileGoCode(
        sourceFile: GoUtFile,
        functions: List<GoUtFunction>,
        eachExecutionTimeoutMillis: Long,
        port: Int,
        maxTraceLength: Int,
        imports: Set<GoImport>
    ): String {
        val destinationPackage = sourceFile.sourcePackage
        val fileCodeBuilder = GoFileCodeBuilder(destinationPackage, imports)

        val workerTestFunctionCode = generateWorkerTestFunctionCode(functions, eachExecutionTimeoutMillis, port)

        val types = functions.flatMap { it.parameters }.map { it.type }
        val aliases = imports.associate { it.goPackage to it.alias }
        val namedTypes = types.getAllVisibleNamedTypes(destinationPackage)

        fileCodeBuilder.addTopLevelElements(
            GoCodeTemplates.getTopLevelHelperStructsAndFunctionsForWorker(
                namedTypes,
                destinationPackage,
                aliases,
                maxTraceLength,
            ) + workerTestFunctionCode
        )

        return fileCodeBuilder.buildCodeString()
    }

    private fun generateFileWithModifiedFunctionsGoCode(sourceFile: GoUtFile, functions: List<GoUtFunction>): String {
        val destinationPackage = sourceFile.sourcePackage
        val imports = functions.fold(emptySet<GoImport>()) { acc, function ->
            acc + function.requiredImports
        }
        val fileCodeBuilder = GoFileCodeBuilder(destinationPackage, imports)
        fileCodeBuilder.addTopLevelElements(
            functions.map { it.modifiedFunctionForCollectingTraces }
        )
        return fileCodeBuilder.buildCodeString()
    }

    private fun generateWorkerTestFunctionCode(
        functions: List<GoUtFunction>, eachExecutionTimeoutMillis: Long, port: Int
    ): String {
        return """
            func $workerTestFunctionName(t *testing.T) {
            	con, err := net.Dial("tcp", ":$port")
            	__checkErrorAndExit__(err)

            	defer func() {
            		err := con.Close()
            		if err != nil {
            			__checkErrorAndExit__(err)
            		}
            	}()

            	jsonDecoder := json.NewDecoder(con)
            	for {
            		funcName, rawValues, err := __parseJsonToFunctionNameAndRawValues__(jsonDecoder)
            		if err == io.EOF {
            			break
            		}
            		__checkErrorAndExit__(err)

            		arguments := __convertRawValuesToReflectValues__(rawValues)

            		var function reflect.Value
            		switch funcName {
            ${
            functions.joinToString(separator = "\n") { function ->
                "case \"${function.modifiedName}\": function = reflect.ValueOf(${function.modifiedName})"
            }
        }
            		default:
            			panic(fmt.Sprintf("no function with that name: %s", funcName))
            		}

            		executionResult := __executeFunction__($eachExecutionTimeoutMillis*time.Millisecond, arguments, func(arguments []reflect.Value) []__RawValue__ {
            			return __wrapResultValuesForUtBotGoWorker__(function.Call(arguments))
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