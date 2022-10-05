package org.utbot.go.gocodeanalyzer

internal data class AnalyzedType(val name: String, val implementsError: Boolean)

internal data class AnalyzedFunctionParameter(val name: String, val type: AnalyzedType)

internal data class AnalyzedFunction(
    val name: String,
    val parameters: List<AnalyzedFunctionParameter>,
    val resultTypes: List<AnalyzedType>,
)

internal data class AnalysisResult(
    val absoluteFilePath: String,
    val packageName: String,
    val analyzedFunctions: List<AnalyzedFunction>,
    val notSupportedFunctionsNames: List<String>,
    val notFoundFunctionsNames: List<String>
)

internal data class AnalysisResults(val results: List<AnalysisResult>)