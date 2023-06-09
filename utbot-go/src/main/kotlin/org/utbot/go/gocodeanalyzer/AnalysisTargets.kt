package org.utbot.go.gocodeanalyzer

internal data class AnalysisTarget(
    val absoluteFilePath: String,
    val targetFunctionNames: List<String>,
    val targetMethodNames: List<String>
)

internal data class AnalysisTargets(val targets: List<AnalysisTarget>)