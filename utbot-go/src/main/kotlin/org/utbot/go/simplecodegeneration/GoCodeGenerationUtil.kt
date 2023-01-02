package org.utbot.go.simplecodegeneration

import org.utbot.go.framework.api.go.GoUtModel
import org.utbot.go.api.*


fun generateFuzzedFunctionCall(functionName: String, fuzzedFunction: GoUtFuzzedFunction): String {
    val fuzzedParameters = fuzzedFunction.fuzzedParametersValues.joinToString {
        when (it.model) {
            is GoUtModel -> it.model.toString()
            else -> throw RuntimeException("${it.model.javaClass} not supported")
        }
    }
    return "${functionName}($fuzzedParameters)"
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
    generateFuzzedFunctionCall(fuzzedFunction.function.name, fuzzedFunction)
)

fun generateCastIfNeed(toTypeId: GoTypeId, expressionType: GoTypeId, expression: String): String {
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