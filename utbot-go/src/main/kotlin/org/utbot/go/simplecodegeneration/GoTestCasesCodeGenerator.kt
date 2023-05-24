package org.utbot.go.simplecodegeneration

import org.utbot.go.api.*
import org.utbot.go.api.util.containsNaNOrInf
import org.utbot.go.api.util.goBoolTypeId
import org.utbot.go.api.util.goFloat64TypeId
import org.utbot.go.api.util.goStringTypeId
import org.utbot.go.framework.api.go.GoImport
import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel
import org.utbot.go.imports.GoImportsResolver

object GoTestCasesCodeGenerator {

    private val alwaysRequiredImports = setOf(
        GoImport(GoPackage("assert", "github.com/stretchr/testify/assert")), GoImport(GoPackage("testing", "testing"))
    )

    private data class Variable(val name: String, val type: GoTypeId, val value: GoUtModel)

    fun generateTestCasesFileCode(sourceFile: GoUtFile, testCases: List<GoUtFuzzedFunctionTestCase>): String {
        val destinationPackage = sourceFile.sourcePackage
        val imports = if (testCases.isEmpty() || testCases.all { it.executionResult is GoUtTimeoutExceeded }) {
            emptySet()
        } else {
            val requiredPackages = mutableSetOf<GoPackage>()
            testCases.forEach { testCase ->
                testCase.parametersValues.forEach {
                    requiredPackages += it.getRequiredPackages(destinationPackage)
                }
                when (val executionResult = testCase.executionResult) {
                    is GoUtExecutionCompleted -> executionResult.models.forEach {
                        requiredPackages += it.getRequiredPackages(destinationPackage)
                    }

                    is GoUtPanicFailure -> requiredPackages += executionResult.panicValue.getRequiredPackages(
                        destinationPackage
                    )
                }
            }

            GoImportsResolver.resolveImportsBasedOnRequiredPackages(
                requiredPackages, destinationPackage, alwaysRequiredImports
            )
        }
        val fileBuilder = GoFileCodeBuilder(destinationPackage, imports)
        val aliases = imports.associate { (goPackage, alias) -> goPackage to alias }
        val goUtModelToCodeConverter = GoUtModelToCodeConverter(destinationPackage, aliases)

        fun List<GoUtFuzzedFunctionTestCase>.generateTestFunctions(
            generateTestFunctionForTestCase: (GoUtFuzzedFunctionTestCase, Int?, GoUtModelToCodeConverter) -> String,
        ) {
            this.forEachIndexed { testIndex, testCase ->
                val testIndexToShow = if (this.size == 1) null else testIndex + 1
                val testFunctionCode =
                    generateTestFunctionForTestCase(testCase, testIndexToShow, goUtModelToCodeConverter)
                fileBuilder.addTopLevelElements(testFunctionCode)
            }
        }

        testCases.groupBy { it.function }.forEach { (_, functionTestCases) ->
            functionTestCases.filter { it.executionResult is GoUtExecutionCompleted }
                .generateTestFunctions(::generateTestFunctionForCompletedExecutionTestCase)
            functionTestCases.filter { it.executionResult is GoUtPanicFailure }
                .generateTestFunctions(::generateTestFunctionForPanicFailureTestCase)
            functionTestCases.filter { it.executionResult is GoUtTimeoutExceeded }
                .generateTestFunctions(::generateTestFunctionForTimeoutExceededTestCase)
        }

        return fileBuilder.buildCodeString()
    }

    private fun generateTestFunctionForCompletedExecutionTestCase(
        testCase: GoUtFuzzedFunctionTestCase, testIndexToShow: Int?, goUtModelToCodeConverter: GoUtModelToCodeConverter
    ): String {
        val (fuzzedFunction, executionResult) = testCase
        val function = fuzzedFunction.function

        val testFunctionNamePostfix = if (executionResult is GoUtExecutionWithNonNilError) {
            "WithNonNilError"
        } else {
            ""
        }
        val testIndexToShowString = testIndexToShow ?: ""
        val testFunctionSignatureDeclaration =
            "func Test${function.name.replaceFirstChar(Char::titlecaseChar)}${testFunctionNamePostfix}ByUtGoFuzzer$testIndexToShowString(t *testing.T)"

        val variables: List<Variable> = generateVariables(fuzzedFunction)
        val variablesDeclarationAndInitialization =
            generateVariablesDeclarationAndInitialization(variables, goUtModelToCodeConverter)

        if (function.results.isEmpty()) {
            val actualFunctionCall = generateFuzzedFunctionCall(function, variables)
            val testFunctionBody = buildString {
                if (variablesDeclarationAndInitialization != "\n") {
                    append(variablesDeclarationAndInitialization)
                }
                appendLine("\tassert.NotPanics(t, func() {")
                appendLine("\t\t$actualFunctionCall")
                appendLine("\t})")

            }
            return "$testFunctionSignatureDeclaration {\n$testFunctionBody}"
        }

        val resultTypes = function.results.map { it.type }
        val doResultTypesImplementError = resultTypes.map { it.implementsError }
        val errorVariablesNumber = doResultTypesImplementError.count { it }
        val commonVariablesNumber = resultTypes.size - errorVariablesNumber
        val actualResultVariablesNames = run {
            var errorVariablesIndex = 0
            var commonVariablesIndex = 0
            doResultTypesImplementError.map { implementsError ->
                if (implementsError) {
                    "actualErr${if (errorVariablesNumber > 1) errorVariablesIndex++ else ""}"
                } else {
                    "actualVal${if (commonVariablesNumber > 1) commonVariablesIndex++ else ""}"
                }
            }
        }
        val actualFunctionCall = generateFuzzedFunctionCallSavedToVariables(
            actualResultVariablesNames, fuzzedFunction, variables
        )

        val expectedResultValues = (executionResult as GoUtExecutionCompleted).models
        val expectedResultVariables = run {
            var errorVariablesIndex = 0
            var commonVariablesIndex = 0
            resultTypes.zip(expectedResultValues).map { (type, value) ->
                if (modelIsNilOrBoolOrFloatNanOrFloatInf(value)) {
                    return@map null
                }
                val name = if (type.implementsError) {
                    "expectedErrorMessage${if (errorVariablesNumber > 1) errorVariablesIndex++ else ""}"
                } else {
                    "expectedVal${if (commonVariablesNumber > 1) commonVariablesIndex++ else ""}"
                }
                Variable(name, type, value)
            }
        }

        val expectedVariablesDeclarationAndInitialization =
            generateVariablesDeclarationAndInitialization(expectedResultVariables, goUtModelToCodeConverter)

        val (assertionName, assertionTParameter) = if (expectedResultValues.size > 1 || expectedResultValues.any { it.isComplexModelAndNeedsSeparateAssertions() }) {
            "assertMultiple" to ""
        } else {
            "assert" to "t, "
        }
        val allAssertionCalls =
            actualResultVariablesNames.zip(expectedResultVariables.map { it?.name ?: "" }).zip(expectedResultValues)
                .flatMap { (actualAndExpectedResultVariableName, expectedResultValue) ->
                    val (actualResultVariableName, expectedResultVariableName) = actualAndExpectedResultVariableName

                    val assertionCalls = if (expectedResultValue.isComplexModelAndNeedsSeparateAssertions()) {
                        listOf(
                            generateCompletedExecutionAssertionCall(
                                expectedModel = expectedResultValue,
                                expectedResultCode = "real($expectedResultVariableName)",
                                actualResultCode = "real($actualResultVariableName)",
                                doesReturnTypeImplementError = expectedResultValue.typeId.implementsError,
                                assertionTParameter
                            ), generateCompletedExecutionAssertionCall(
                                expectedModel = expectedResultValue,
                                expectedResultCode = "imag($expectedResultVariableName)",
                                actualResultCode = "imag($actualResultVariableName)",
                                doesReturnTypeImplementError = expectedResultValue.typeId.implementsError,
                                assertionTParameter
                            )
                        )
                    } else {
                        listOf(
                            generateCompletedExecutionAssertionCall(
                                expectedModel = expectedResultValue,
                                expectedResultCode = expectedResultVariableName,
                                actualResultCode = actualResultVariableName,
                                doesReturnTypeImplementError = expectedResultValue.typeId.implementsError,
                                assertionTParameter
                            )
                        )
                    }
                    assertionCalls.map { "$assertionName.$it" }
                }

        val testFunctionBody = buildString {
            if (variablesDeclarationAndInitialization != "\n") {
                append(variablesDeclarationAndInitialization)
            }
            append("\t$actualFunctionCall\n\n")
            if (expectedVariablesDeclarationAndInitialization != "\n") {
                append(expectedVariablesDeclarationAndInitialization)
            }
            if (expectedResultValues.size > 1 || expectedResultValues.any { it.isComplexModelAndNeedsSeparateAssertions() }) {
                append("\tassertMultiple := assert.New(t)\n")
            }
            allAssertionCalls.forEach {
                append("\t$it\n")
            }
        }

        return "$testFunctionSignatureDeclaration {\n$testFunctionBody}"
    }

    private fun GoUtModel.isComplexModelAndNeedsSeparateAssertions(): Boolean =
        this is GoUtComplexModel && this.containsNaNOrInf()

    private fun generateCompletedExecutionAssertionCall(
        expectedModel: GoUtModel,
        expectedResultCode: String,
        actualResultCode: String,
        doesReturnTypeImplementError: Boolean,
        assertionTParameter: String,
    ): String {
        if (expectedModel is GoUtNilModel || (expectedModel is GoUtNamedModel && expectedModel.value is GoUtNilModel)) {
            return "Nil($assertionTParameter$actualResultCode)"
        }
        if (doesReturnTypeImplementError && (expectedModel is GoUtNamedModel && expectedModel.value.typeId == goStringTypeId)) {
            return "ErrorContains($assertionTParameter$actualResultCode, ${expectedResultCode})"
        }
        if (expectedModel is GoUtPrimitiveModel && expectedModel.typeId == goBoolTypeId) {
            return if (expectedModel.value == true) {
                "True($assertionTParameter$actualResultCode)"
            } else {
                "False($assertionTParameter$actualResultCode)"
            }
        }
        if (expectedModel is GoUtFloatNaNModel) {
            val castedActualResultCode = generateCastIfNeed(goFloat64TypeId, expectedModel.typeId, actualResultCode)
            return "True(${assertionTParameter}math.IsNaN($castedActualResultCode))"
        }
        if (expectedModel is GoUtFloatInfModel) {
            val castedActualResultCode = generateCastIfNeed(goFloat64TypeId, expectedModel.typeId, actualResultCode)
            return "True(${assertionTParameter}math.IsInf($castedActualResultCode, ${expectedModel.sign}))"
        }
        val prefix = if (!expectedModel.isComparable()) "Not" else ""
        return "${prefix}Equal($assertionTParameter$expectedResultCode, $actualResultCode)"
    }

    private fun generateTestFunctionForPanicFailureTestCase(
        testCase: GoUtFuzzedFunctionTestCase, testIndexToShow: Int?, goUtModelToCodeConverter: GoUtModelToCodeConverter
    ): String {
        val (fuzzedFunction, executionResult) = testCase
        val function = fuzzedFunction.function

        val testIndexToShowString = testIndexToShow ?: ""
        val testFunctionSignatureDeclaration =
            "func Test${function.name.replaceFirstChar(Char::titlecaseChar)}PanicsByUtGoFuzzer$testIndexToShowString(t *testing.T)"

        val variables: List<Variable> = generateVariables(fuzzedFunction)
        val variablesDeclaration = generateVariablesDeclarationAndInitialization(variables, goUtModelToCodeConverter)

        val actualFunctionCall = generateFuzzedFunctionCall(function, variables)
        val actualFunctionCallLambda = buildString {
            appendLine("func() {")
            if (function.results.isNotEmpty()) {
                appendLine("\t\t${function.results.joinToString { "_" }} = $actualFunctionCall")
            } else {
                appendLine("\t\t$actualFunctionCall")
            }
            append("\t}")
        }
        val testFunctionBodySb = StringBuilder(variablesDeclaration)
        val (expectedPanicValue, isErrorMessage) = (executionResult as GoUtPanicFailure)
        val panicValueIsComparable = expectedPanicValue.isComparable()
        if (isErrorMessage) {
            val errorMessageVariable = Variable("expectedErrorMessage", expectedPanicValue.typeId, expectedPanicValue)

            val errorMessageToGoCode = goUtModelToCodeConverter.toGoCode(errorMessageVariable.value)
            testFunctionBodySb.append("\t${errorMessageVariable.name} := ${errorMessageToGoCode}\n\n")
            testFunctionBodySb.append("\tassert.PanicsWithError(t, ${errorMessageVariable.name}, $actualFunctionCallLambda)\n")
        } else if (panicValueIsComparable || expectedPanicValue is GoUtNilModel) {
            val panicValueVariable = Variable("expectedVal", expectedPanicValue.typeId, expectedPanicValue)

            val panicValueToGoCode = goUtModelToCodeConverter.toGoCode(panicValueVariable.value)
            testFunctionBodySb.append("\t${panicValueVariable.name} := $panicValueToGoCode\n\n")
            testFunctionBodySb.append("\tassert.PanicsWithValue(t, ${panicValueVariable.name}, $actualFunctionCallLambda)\n")
        } else {
            testFunctionBodySb.append("\tassert.Panics(t, $actualFunctionCallLambda)\n")
        }

        val testFunctionBody = testFunctionBodySb.toString()
        return "$testFunctionSignatureDeclaration {\n$testFunctionBody}"
    }

    private fun generateTestFunctionForTimeoutExceededTestCase(
        testCase: GoUtFuzzedFunctionTestCase,
        @Suppress("UNUSED_PARAMETER") testIndexToShow: Int?,
        goUtModelToCodeConverter: GoUtModelToCodeConverter
    ): String {
        val (fuzzedFunction, executionResult) = testCase
        val functionName = fuzzedFunction.function.name
        val fuzzedParametersToString = fuzzedFunction.parametersValues.joinToString {
            goUtModelToCodeConverter.toGoCode(it)
        }
        val actualFunctionCall = "$functionName($fuzzedParametersToString)"
        val exceededTimeoutMillis = (executionResult as GoUtTimeoutExceeded).timeoutMillis
        return "// $actualFunctionCall exceeded $exceededTimeoutMillis ms timeout"
    }

    private fun generateVariables(fuzzedFunction: GoUtFuzzedFunction): List<Variable> {
        val function = fuzzedFunction.function
        val parameters = if (function.isMethod) {
            listOf(function.receiver!!) + function.parameters
        } else {
            function.parameters
        }
        val parametersValues = fuzzedFunction.parametersValues
        return parameters.map { it.name }.zip(parameters.map { it.type }).zip(parametersValues)
            .map { (nameAndType, value) ->
                val (name, type) = nameAndType
                Variable(name, type, value)
            }
    }

    private fun generateChannelInitialization(
        nameOfVariable: String,
        model: GoUtChanModel,
        goUtModelToCodeConverter: GoUtModelToCodeConverter
    ): String = model.getElements().joinToString(separator = "") {
        "\t$nameOfVariable <- ${goUtModelToCodeConverter.toGoCode(it)}\n"
    } + "\tclose($nameOfVariable)\n"

    private fun generatePointerToPrimitiveInitialization(
        nameOfVariable: String,
        model: GoUtPrimitiveModel,
        goUtModelToCodeConverter: GoUtModelToCodeConverter
    ): String = "\t*$nameOfVariable = ${goUtModelToCodeConverter.toGoCodeWithoutTypeName(model)}\n"

    private fun generateVariableDeclaration(
        variable: Variable,
        goUtModelToCodeConverter: GoUtModelToCodeConverter
    ): String {
        val (name, type, value) = variable
        return if (type.implementsError && (value is GoUtNamedModel && value.value.typeId == goStringTypeId)) {
            "\t$name := ${goUtModelToCodeConverter.toGoCode(value.value)}\n"
        } else {
            "\t$name := ${goUtModelToCodeConverter.toGoCode(value)}\n"
        }
    }

    private fun generateVariableInitialization(
        variable: Variable,
        goUtModelToCodeConverter: GoUtModelToCodeConverter
    ): String {
        val (name, _, value) = variable
        return when (value) {
            is GoUtChanModel -> generateChannelInitialization(name, value, goUtModelToCodeConverter)

            is GoUtNamedModel -> if (value.value is GoUtChanModel) {
                generateChannelInitialization(name, value.value as GoUtChanModel, goUtModelToCodeConverter)
            } else {
                ""
            }

            is GoUtPointerModel -> if (value.value is GoUtPrimitiveModel) {
                generatePointerToPrimitiveInitialization(
                    name,
                    value.value as GoUtPrimitiveModel,
                    goUtModelToCodeConverter
                )
            } else if (value.value is GoUtNamedModel && (value.value as GoUtNamedModel).value is GoUtPrimitiveModel) {
                generatePointerToPrimitiveInitialization(
                    name,
                    (value.value as GoUtNamedModel).value as GoUtPrimitiveModel,
                    goUtModelToCodeConverter
                )
            } else {
                ""
            }

            else -> ""
        }
    }

    private fun generateVariablesDeclarationAndInitialization(
        variables: List<Variable?>, goUtModelToCodeConverter: GoUtModelToCodeConverter
    ): String {
        val vars = variables.filterNotNull()
        return if (vars.isNotEmpty()) {
            vars.joinToString(separator = "", postfix = "\n") { variable ->
                val declaration = generateVariableDeclaration(variable, goUtModelToCodeConverter)
                val initialization = generateVariableInitialization(variable, goUtModelToCodeConverter)
                declaration + initialization
            }
        } else {
            ""
        }
    }

    private fun generateFuzzedFunctionCall(function: GoUtFunction, variables: List<Variable>): String {
        return if (function.isMethod) {
            val fuzzedParametersToString = variables.drop(1).joinToString(prefix = "(", postfix = ")") {
                it.name
            }
            "${variables[0].name}.${function.name}$fuzzedParametersToString"
        } else {
            val fuzzedParametersToString = variables.joinToString(prefix = "(", postfix = ")") {
                it.name
            }
            "${function.name}$fuzzedParametersToString"
        }
    }

    private fun generateVariablesDeclarationTo(variablesNames: List<String>, expression: String): String {
        val variables = variablesNames.joinToString()
        return "$variables := $expression"
    }

    private fun generateFuzzedFunctionCallSavedToVariables(
        variablesNames: List<String>, fuzzedFunction: GoUtFuzzedFunction, variables: List<Variable>
    ): String = generateVariablesDeclarationTo(
        variablesNames, expression = generateFuzzedFunctionCall(fuzzedFunction.function, variables)
    )

    private fun modelIsNilOrBoolOrFloatNanOrFloatInf(value: GoUtModel): Boolean = value is GoUtNilModel
            || (value is GoUtNamedModel && value.value is GoUtNilModel)
            || value.typeId == goBoolTypeId
            || value is GoUtFloatNaNModel
            || value is GoUtFloatInfModel

    private fun generateCastIfNeed(
        toTypeId: GoPrimitiveTypeId, expressionType: GoPrimitiveTypeId, expression: String
    ): String {
        return if (expressionType != toTypeId) {
            "${toTypeId.name}($expression)"
        } else {
            expression
        }
    }
}