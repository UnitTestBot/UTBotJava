package org.utbot.go.gocodeinstrumentation

internal data class InstrumentationTarget(val absolutePackagePath: String, val testedFunctions: List<String>)