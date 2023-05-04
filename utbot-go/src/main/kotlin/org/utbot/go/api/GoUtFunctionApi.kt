package org.utbot.go.api

import org.utbot.go.framework.api.go.GoPackage
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel
import java.io.File
import java.nio.file.Paths

data class GoUtFile(val absolutePath: String, val sourcePackage: GoPackage) {
    val fileName: String get() = File(absolutePath).name
    val fileNameWithoutExtension: String get() = File(absolutePath).nameWithoutExtension
    val absoluteDirectoryPath: String get() = Paths.get(absolutePath).parent.toString()
}

data class GoUtFunctionParameter(val name: String, val type: GoTypeId)

data class GoUtFunction(
    val name: String,
    val parameters: List<GoUtFunctionParameter>,
    val resultTypes: List<GoTypeId>,
    val constants: Map<GoTypeId, List<Any>>,
    val sourceFile: GoUtFile
) {
    val sourcePackage: GoPackage = sourceFile.sourcePackage

    fun getPackageName(): String = sourceFile.sourcePackage.packageName
}

data class GoUtFuzzedFunction(val function: GoUtFunction, val parametersValues: List<GoUtModel>)

data class GoUtFuzzedFunctionTestCase(
    val fuzzedFunction: GoUtFuzzedFunction,
    val executionResult: GoUtExecutionResult,
) {
    val function: GoUtFunction get() = fuzzedFunction.function
    val parametersValues: List<GoUtModel> get() = fuzzedFunction.parametersValues
}