package org.utbot.go.simplecodegeneration

import org.utbot.go.api.ExplicitCastMode
import org.utbot.go.api.GoTypeId
import org.utbot.go.api.GoUtFuzzedFunction
import org.utbot.go.api.GoUtPrimitiveModel

fun generateFuzzedFunctionCall(fuzzedFunction: GoUtFuzzedFunction): String {
    val fuzzedParameters = fuzzedFunction.fuzzedParametersValues.joinToString(separator = ", ") {
        (it.model as GoUtPrimitiveModel).toString()
    }
    return "${fuzzedFunction.function.name}($fuzzedParameters)"
}

fun generateVariablesDeclarationTo(variablesNames: List<String>, expression: String): String {
    val variables = variablesNames.joinToString(separator = ", ")
    return "$variables := $expression"
}

fun generateFuzzedFunctionCallSavedToVariables(
    variablesNames: List<String>,
    fuzzedFunction: GoUtFuzzedFunction
): String = generateVariablesDeclarationTo(
    variablesNames,
    generateFuzzedFunctionCall(fuzzedFunction)
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