package org.utbot.go.executor

import org.utbot.go.api.GoUtFile
import org.utbot.go.api.GoUtFuzzedFunction
import org.utbot.go.simplecodegeneration.GoFileCodeBuilder
import org.utbot.go.simplecodegeneration.generateFuzzedFunctionCall
import org.utbot.go.util.goRequiredImports

internal object GoFuzzedFunctionsExecutorCodeGenerationHelper {

    private val alwaysRequiredImports = setOf("encoding/json", "fmt", "math", "os", "testing", "reflect")

    fun generateExecutorTestFileGoCode(
        sourceFile: GoUtFile,
        fuzzedFunctions: List<GoUtFuzzedFunction>,
        executorTestFunctionName: String,
        rawExecutionResultsFileName: String,
    ): String {
        val fileCodeBuilder = GoFileCodeBuilder()

        fileCodeBuilder.setPackage(sourceFile.packageName)

        val additionalImports = mutableSetOf<String>()
        fuzzedFunctions.forEach { (_, fuzzedParametersValues) ->
            fuzzedParametersValues.forEach { additionalImports += it.goRequiredImports }
        }
        fileCodeBuilder.setImports(alwaysRequiredImports + additionalImports)

        val executorTestFunctionCode =
            generateExecutorTestFunctionCode(fuzzedFunctions, executorTestFunctionName, rawExecutionResultsFileName)
        fileCodeBuilder.addTopLevelElements(
            CodeTemplates.topLevelHelperStructsAndFunctions + listOf(executorTestFunctionCode)
        )

        return fileCodeBuilder.buildCodeString()
    }

    // TODO: use more convenient code generation
    private fun generateExecutorTestFunctionCode(
        fuzzedFunctions: List<GoUtFuzzedFunction>,
        executorTestFunctionName: String,
        rawExecutionResultsFileName: String,
    ): String {
        val codeSb = StringBuilder()
        codeSb.append("func $executorTestFunctionName(t *testing.T) {")
        codeSb.append("\n\texecutionResults := __UtBotGoExecutorRawExecutionResults__{Results: []__UtBotGoExecutorRawExecutionResult__{")

        fuzzedFunctions.forEach { fuzzedFunction ->
            val fuzzedFunctionCall = generateFuzzedFunctionCall(fuzzedFunction)
            val function = fuzzedFunction.function
            codeSb.append("\n\t\t__executeFunctionForUtBotGoExecutor__(\"${function.name}\", func() []*string {")
            if (function.resultTypes.isEmpty()) {
                codeSb.append("\n\t\t\t$fuzzedFunctionCall")
                codeSb.append("\n\t\t\treturn []*string{}")
            } else {
                codeSb.append("\n\t\t\treturn __wrapResultValuesForUtBotGoExecutor__($fuzzedFunctionCall)")
            }
            codeSb.append("\n\t\t}),")
        }

        codeSb.append("\n")
        codeSb.append(
            """
                }}
                
            	jsonBytes, toJsonErr := json.MarshalIndent(executionResults, "", "  ")
            	__checkErrorAndExitToUtBotGoExecutor__(toJsonErr)

            	const resultsFilePath = "$rawExecutionResultsFileName"
            	writeErr := os.WriteFile(resultsFilePath, jsonBytes, os.ModePerm)
            	__checkErrorAndExitToUtBotGoExecutor__(writeErr)
            }
            
        """.trimIndent()
        )

        return codeSb.toString()
    }

    private object CodeTemplates {

        private val panicMessageStruct = """
            type __UtBotGoExecutorRawPanicMessage__ struct {
            	RawValue        *string `json:"rawValue"`
            	GoTypeName      string  `json:"goTypeName"`
            	ImplementsError bool    `json:"implementsError"`
            }
        """.trimIndent()

        private val rawExecutionResultStruct = """
            type __UtBotGoExecutorRawExecutionResult__ struct {
            	FunctionName    string                              `json:"functionName"`
            	ResultRawValues []*string                           `json:"resultRawValues"`
            	PanicMessage    *__UtBotGoExecutorRawPanicMessage__ `json:"panicMessage"`
            }
        """.trimIndent()

        private val rawExecutionResultsStruct = """
            type __UtBotGoExecutorRawExecutionResults__ struct {
            	Results []__UtBotGoExecutorRawExecutionResult__ `json:"results"`
            }
        """.trimIndent()

        private val checkErrorFunction = """
            func __checkErrorAndExitToUtBotGoExecutor__(err error) {
            	if err != nil {
            		os.Exit(1)
            	}
            }
        """.trimIndent()

        private val convertFloat64ValueToStringFunction = """
            func __convertFloat64ValueToStringForUtBotGoExecutor__(value float64) string {
            	const outputNaN = "NaN"
            	const outputPosInf = "+Inf"
            	const outputNegInf = "-Inf"
            	switch {
            	case math.IsNaN(value):
            		return fmt.Sprint(outputNaN)
            	case math.IsInf(value, 1):
            		return fmt.Sprint(outputPosInf)
            	case math.IsInf(value, -1):
            		return fmt.Sprint(outputNegInf)
            	default:
            		return fmt.Sprintf("%#v", value)
            	}
            }
        """.trimIndent()

        private val convertFloat32ValueToStringFunction = """
            func __convertFloat32ValueToStringForUtBotGoExecutor__(value float32) string {
            	return __convertFloat64ValueToStringForUtBotGoExecutor__(float64(value))
            }
        """.trimIndent()

        private val convertValueToStringFunction = """
            func __convertValueToStringForUtBotGoExecutor__(value any) string {
            	if typedValue, ok := value.(error); ok {
            		return fmt.Sprintf("%#v", typedValue.Error())
            	}
            	const outputComplexPartsDelimiter = "@"
            	switch typedValue := value.(type) {
            	case complex128:
            		realPartString := __convertFloat64ValueToStringForUtBotGoExecutor__(real(typedValue))
            		imagPartString := __convertFloat64ValueToStringForUtBotGoExecutor__(imag(typedValue))
            		return fmt.Sprintf("%v%v%v", realPartString, outputComplexPartsDelimiter, imagPartString)
            	case complex64:
            		realPartString := __convertFloat32ValueToStringForUtBotGoExecutor__(real(typedValue))
            		imagPartString := __convertFloat32ValueToStringForUtBotGoExecutor__(imag(typedValue))
            		return fmt.Sprintf("%v%v%v", realPartString, outputComplexPartsDelimiter, imagPartString)
            	case float64:
            		return __convertFloat64ValueToStringForUtBotGoExecutor__(typedValue)
            	case float32:
            		return __convertFloat32ValueToStringForUtBotGoExecutor__(typedValue)
            	case string:
            		return fmt.Sprintf("%#v", typedValue)
            	default:
            		return fmt.Sprintf("%v", typedValue)
            	}
            }
        """.trimIndent()

        private val convertValueToRawValueFunction = """
            func __convertValueToRawValueForUtBotGoExecutor__(value any) *string {
            	if value == nil {
            		return nil
            	} else {
            		rawValue := __convertValueToStringForUtBotGoExecutor__(value)
            		return &rawValue
            	}
            }
        """.trimIndent()

        private val getValueRawGoTypeFunction = """
            func __getValueRawGoTypeForUtBotGoExecutor__(value any) string {
            	return __convertValueToStringForUtBotGoExecutor__(reflect.TypeOf(value))
            }
        """.trimIndent()

        private val executeFunctionFunction = """
            func __executeFunctionForUtBotGoExecutor__(functionName string, wrappedFunction func() []*string) (
            	executionResult __UtBotGoExecutorRawExecutionResult__,
            ) {
            	executionResult.FunctionName = functionName
            	executionResult.ResultRawValues = []*string{}
            	panicked := true
            	defer func() {
            		panicMessage := recover()
            		if panicked {
            			_, implementsError := panicMessage.(error)
            			executionResult.PanicMessage = &__UtBotGoExecutorRawPanicMessage__{
            				RawValue:        __convertValueToRawValueForUtBotGoExecutor__(panicMessage),
            				GoTypeName:      __getValueRawGoTypeForUtBotGoExecutor__(panicMessage),
            				ImplementsError: implementsError,
            			}
            		} else {
            			executionResult.PanicMessage = nil
            		}
            	}()

            	rawResultValues := wrappedFunction()
            	executionResult.ResultRawValues = rawResultValues
            	panicked = false

            	return executionResult
            }
        """.trimIndent()

        private val wrapResultValuesFunction = """
            //goland:noinspection GoPreferNilSlice
            func __wrapResultValuesForUtBotGoExecutor__(values ...any) []*string {
            	rawValues := []*string{}
            	for _, value := range values {
            		rawValues = append(rawValues, __convertValueToRawValueForUtBotGoExecutor__(value))
            	}
            	return rawValues
            }
        """.trimIndent()

        val topLevelHelperStructsAndFunctions = listOf(
            panicMessageStruct,
            rawExecutionResultStruct,
            rawExecutionResultsStruct,
            checkErrorFunction,
            convertFloat64ValueToStringFunction,
            convertFloat32ValueToStringFunction,
            convertValueToStringFunction,
            convertValueToRawValueFunction,
            getValueRawGoTypeFunction,
            executeFunctionFunction,
            wrapResultValuesFunction,
        )
    }
}