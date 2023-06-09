package org.utbot.go.gocodeinstrumentation

data class InstrumentationResult(
    val absolutePathToInstrumentedPackage: String,
    val absolutePathToInstrumentedModule: String,
    val testedFunctionsToCounters: Map<String, List<String>>
)