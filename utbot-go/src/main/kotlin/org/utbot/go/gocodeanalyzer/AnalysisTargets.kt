package org.utbot.go.gocodeanalyzer

internal data class AnalysisTarget(val absoluteFilePath: String, val targetFunctionsNames: List<String>)

internal data class AnalysisTargets(val targets: List<AnalysisTarget>)