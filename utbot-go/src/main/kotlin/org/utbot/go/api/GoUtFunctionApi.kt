package org.utbot.go.api

import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedValue
import org.utbot.go.framework.api.go.GoTypeId
import java.io.File
import java.nio.file.Paths

data class GoUtFile(val absolutePath: String, val packageName: String) {
    val fileName: String get() = File(absolutePath).name
    val fileNameWithoutExtension: String get() = File(absolutePath).nameWithoutExtension
    val absoluteDirectoryPath: String get() = Paths.get(absolutePath).parent.toString()
}

data class GoUtFunctionParameter(val name: String, val type: GoTypeId)

data class GoUtFunction(
    val name: String,
    val modifiedName: String,
    val parameters: List<GoUtFunctionParameter>,
    val resultTypes: List<GoTypeId>,
    val concreteValues: Collection<FuzzedConcreteValue>,
    val modifiedFunctionForCollectingTraces: String,
    val numberOfAllStatements: Int,
    val sourceFile: GoUtFile
) {
    fun getPackageName(): String = sourceFile.packageName
}

data class GoUtFuzzedFunction(val function: GoUtFunction, val fuzzedParametersValues: List<FuzzedValue>)

data class GoUtFuzzedFunctionTestCase(
    val fuzzedFunction: GoUtFuzzedFunction,
    val executionResult: GoUtExecutionResult,
) {
    val function: GoUtFunction get() = fuzzedFunction.function
    val fuzzedParametersValues: List<FuzzedValue> get() = fuzzedFunction.fuzzedParametersValues
}