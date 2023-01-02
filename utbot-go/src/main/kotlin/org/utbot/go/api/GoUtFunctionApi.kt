package org.utbot.go.api

import org.utbot.go.framework.api.go.GoClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
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
    private val sourceFile: GoUtFile
) {
    val parametersNames: List<String> get() = parameters.map { it.name }
    val parametersTypes: List<GoTypeId> get() = parameters.map { it.type }

    val resultTypesAsGoClassId: GoClassId
        get() = if (resultTypes.isEmpty()) GoSyntheticNoTypeId()
        else if (resultTypes.size == 1) resultTypes.first()
        else GoSyntheticMultipleTypesId(resultTypes)

    fun toFuzzedMethodDescription() =
        FuzzedMethodDescription(name, resultTypesAsGoClassId, parametersTypes, concreteValues).apply {
            compilableName = name
            val names = parametersNames
            parameterNameMap = { index -> names.getOrNull(index) }
        }
}

data class GoUtFuzzedFunction(val function: GoUtFunction, val fuzzedParametersValues: List<FuzzedValue>)

data class GoUtFuzzedFunctionTestCase(
    val fuzzedFunction: GoUtFuzzedFunction,
    val executionResult: GoUtExecutionResult,
) {
    val function: GoUtFunction get() = fuzzedFunction.function
    val fuzzedParametersValues: List<FuzzedValue> get() = fuzzedFunction.fuzzedParametersValues
}