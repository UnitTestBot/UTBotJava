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
        GoPackage("binary", "encoding/binary"),
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
        absoluteInstrumentedPackagePath: String,
        eachExecutionTimeoutMillis: Long,
        port: Int,
        imports: Set<GoImport>
    ): File {
        val fileToExecuteName = createFileToExecuteName()
        val sourceFileDir = File(absoluteInstrumentedPackagePath)
        val fileToExecute = sourceFileDir.resolve(fileToExecuteName)

        val fileToExecuteGoCode =
            generateWorkerTestFileGoCode(
                sourceFile,
                functions,
                eachExecutionTimeoutMillis,
                port,
                imports
            )
        fileToExecute.writeText(fileToExecuteGoCode)
        return fileToExecute
    }

    fun createFileWithCoverTab(
        sourceFile: GoUtFile,
        absoluteInstrumentedPackagePath: String,
    ): File {
        val fileWithCoverTabName = createFileWithCoverTabName()
        val sourceFileDir = File(absoluteInstrumentedPackagePath)
        val fileWithCoverTab = sourceFileDir.resolve(fileWithCoverTabName)

        val fileWithCoverTabGoCode = generateFileWithCoverTabGoCode(sourceFile.sourcePackage)
        fileWithCoverTab.writeText(fileWithCoverTabGoCode)
        return fileWithCoverTab
    }

    private fun createFileToExecuteName(): String {
        return "utbot_go_worker_test.go"
    }

    private fun createFileWithCoverTabName(): String {
        return "utbot_go_cover.go"
    }

    private fun generateWorkerTestFileGoCode(
        sourceFile: GoUtFile,
        functions: List<GoUtFunction>,
        eachExecutionTimeoutMillis: Long,
        port: Int,
        imports: Set<GoImport>
    ): String {
        val destinationPackage = sourceFile.sourcePackage
        val fileCodeBuilder = GoFileCodeBuilder(destinationPackage, imports)

        val types = functions.flatMap {
            it.parameters + if (it.isMethod) listOf(it.receiver!!) else emptyList()
        }.map { it.type }
        val aliases = imports.associate { it.goPackage to it.alias }
        val namedTypes = types.getAllVisibleNamedTypes(destinationPackage)

        val workerTestFunctionCode = generateWorkerTestFunctionCode(
            functions, destinationPackage, aliases, eachExecutionTimeoutMillis, port
        )
        fileCodeBuilder.addTopLevelElements(
            GoCodeTemplates.getTopLevelHelperStructsAndFunctionsForWorker(
                namedTypes,
                destinationPackage,
                aliases,
            ) + workerTestFunctionCode
        )

        return fileCodeBuilder.buildCodeString()
    }

    private fun generateFileWithCoverTabGoCode(goPackage: GoPackage): String = """
        package ${goPackage.name}

        const __CoverSize__ = 64 << 10

        var __CoverTab__ []int
    """.trimIndent()

    private fun generateWorkerTestFunctionCode(
        functions: List<GoUtFunction>,
        destinationPackage: GoPackage,
        aliases: Map<GoPackage, String?>,
        eachExecutionTimeoutMillis: Long,
        port: Int
    ): String {
        val functionNameToFunctionCall = functions.map { function ->
            function.name to if (function.isMethod) {
                "(${function.receiver!!.type.getRelativeName(destinationPackage, aliases)}).${function.name}"
            } else {
                function.name
            }
        }
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
            functionNameToFunctionCall.joinToString(separator = "\n") { (functionName, functionCall) ->
                "case \"${functionName}\": function = reflect.ValueOf($functionCall)"
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

            		bs := make([]byte, 4)
            		binary.BigEndian.PutUint32(bs, uint32(len(jsonBytes)))
            		_, err = con.Write(bs)
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