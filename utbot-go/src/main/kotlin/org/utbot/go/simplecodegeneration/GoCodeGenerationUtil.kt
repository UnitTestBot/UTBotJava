package org.utbot.go.simplecodegeneration

import org.utbot.fuzzer.FuzzedValue
import org.utbot.go.api.ExplicitCastMode
import org.utbot.go.api.GoPrimitiveTypeId
import org.utbot.go.api.GoUtFuzzedFunction
import org.utbot.go.api.GoUtPrimitiveModel


fun generateFuzzedFunctionCall(functionName: String, fuzzedParameters: List<FuzzedValue>): String {
    val fuzzedParametersToString = fuzzedParameters.joinToString(prefix = "(", postfix = ")") { it.model.toString() }
    return "$functionName$fuzzedParametersToString"
}

fun generateVariablesDeclarationTo(variablesNames: List<String>, expression: String): String {
    val variables = variablesNames.joinToString()
    return "$variables := $expression"
}

fun generateFuzzedFunctionCallSavedToVariables(
    variablesNames: List<String>,
    fuzzedFunction: GoUtFuzzedFunction
): String = generateVariablesDeclarationTo(
    variablesNames,
    generateFuzzedFunctionCall(fuzzedFunction.function.name, fuzzedFunction.fuzzedParametersValues)
)

fun generateCastIfNeed(toTypeId: GoPrimitiveTypeId, expressionType: GoPrimitiveTypeId, expression: String): String {
    return if (expressionType != toTypeId) {
        "${toTypeId.name}($expression)"
    } else {
        expression
    }
}

fun generateCastedValueIfPossible(model: GoUtPrimitiveModel): String {
    return if (model.explicitCastMode == ExplicitCastMode.NEVER) {
        model.toValueGoCode()
    } else {
        model.toCastedValueGoCode()
    }
}