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
        GoPackage("os", "os"),
        GoPackage("context", "context"),
        GoPackage("json", "encoding/json"),
        GoPackage("fmt", "fmt"),
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
            	if err != nil {
            		_, _ = fmt.Fprintf(os.Stderr, "Connection to server failed: %s", err)
            		os.Exit(1)
            	}

            	defer func() {
            		err = con.Close()
            		if err != nil {
            			_, _ = fmt.Fprintf(os.Stderr, "Closing connection failed: %s", err)
            			os.Exit(1)
            		}
            	}()

            	jsonDecoder := json.NewDecoder(con)
            	for {
            		var (
            			funcName  string
            			rawValues []__RawValue__
            		)
            		funcName, rawValues, err = __parseTestInput__(jsonDecoder)
            		if err == io.EOF {
            			break
            		}
            		if err != nil {
            			_, _ = fmt.Fprintf(os.Stderr, "Failed to parse test input: %s", err)
            			os.Exit(1)
            		}

            		var arguments []reflect.Value
            		arguments, err = __convertRawValuesToReflectValues__(rawValues)
            		if err != nil {
            			_, _ = fmt.Fprintf(os.Stderr, "Failed to convert slice of RawValue to slice of reflect.Value: %s", err)
            			os.Exit(1)
            		}

            		var function reflect.Value
            		switch funcName {
            ${
            functions.joinToString(separator = "\n") { function ->
                "case \"${function.modifiedName}\": function = reflect.ValueOf(${function.modifiedName})"
            }
        }
            		default:
            			_, _ = fmt.Fprintf(os.Stderr, "Function %s not found", funcName)
            			os.Exit(1)
            		}

            		executionResult := __executeFunction__(function, arguments, $eachExecutionTimeoutMillis*time.Millisecond)

            		var jsonBytes []byte
            		jsonBytes, err = json.Marshal(executionResult)
            		if err != nil {
            			_, _ = fmt.Fprintf(os.Stderr, "Failed to serialize execution result to json: %s", err)
            			os.Exit(1)
            		}

            		_, err = con.Write([]byte(strconv.Itoa(len(jsonBytes)) + "\n"))
            		if err != nil {
            			_, _ = fmt.Fprintf(os.Stderr, "Failed to send length of execution result: %s", err)
            			os.Exit(1)
            		}

            		_, err = con.Write(jsonBytes)
            		if err != nil {
            			_, _ = fmt.Fprintf(os.Stderr, "Failed to send execution result: %s", err)
            			os.Exit(1)
            		}
            	}
            }
        """.trimIndent()
    }
}