package org.utbot.go.executor

import org.utbot.go.api.*
import org.utbot.go.api.util.*
import org.utbot.go.framework.api.go.GoUtModel
import org.utbot.go.logic.EachExecutionTimeoutsMillisConfig
import org.utbot.go.util.executeCommandByNewProcessOrFail
import org.utbot.go.util.parseFromJsonOrFail
import java.io.File

object GoFuzzedFunctionsExecutor {

    fun executeGoSourceFileFuzzedFunctions(
        sourceFile: GoUtFile,
        fuzzedFunction: GoUtFuzzedFunction,
        goExecutableAbsolutePath: String,
        eachExecutionTimeoutsMillisConfig: EachExecutionTimeoutsMillisConfig
    ): GoUtExecutionResult {
        val fileToExecuteName = createFileToExecuteName(sourceFile)
        val rawExecutionResultsFileName = createRawExecutionResultsFileName(sourceFile)

        val sourceFileDir = File(sourceFile.absoluteDirectoryPath)
        val fileToExecute = sourceFileDir.resolve(fileToExecuteName)
        val rawExecutionResultsFile = sourceFileDir.resolve(rawExecutionResultsFileName)

        val executorTestFunctionName = createExecutorTestFunctionName()
        val runGeneratedGoExecutorTestCommand = listOf(
            goExecutableAbsolutePath, "test", "-run", executorTestFunctionName
        )

        try {
            val fileToExecuteGoCode = GoFuzzedFunctionsExecutorCodeGenerationHelper.generateExecutorTestFileGoCode(
                sourceFile,
                fuzzedFunction,
                eachExecutionTimeoutsMillisConfig,
                executorTestFunctionName,
                rawExecutionResultsFileName
            )
            fileToExecute.writeText(fileToExecuteGoCode)

            executeCommandByNewProcessOrFail(
                runGeneratedGoExecutorTestCommand,
                sourceFileDir,
                "functions from $sourceFile",
                StringBuilder().append("Try reducing the timeout for each function execution, ")
                    .append("select fewer functions for test generation at the same time, ")
                    .append("or handle corner cases in the source code. ")
                    .append("Perhaps some functions are too resource-intensive.").toString()
            )
            val rawExecutionResults = parseFromJsonOrFail<RawExecutionResults>(rawExecutionResultsFile)

            return convertRawExecutionResultToExecutionResult(
                rawExecutionResults.results.first(),
                fuzzedFunction.function.resultTypes,
                eachExecutionTimeoutsMillisConfig[fuzzedFunction.function],
            )
        } finally {
            fileToExecute.delete()
            rawExecutionResultsFile.delete()
        }
    }

    private fun createFileToExecuteName(sourceFile: GoUtFile): String {
        return "utbot_go_executor_${sourceFile.fileNameWithoutExtension}_test.go"
    }

    private fun createRawExecutionResultsFileName(sourceFile: GoUtFile): String {
        return "utbot_go_executor_${sourceFile.fileNameWithoutExtension}_test_results.json"
    }

    private fun createExecutorTestFunctionName(): String {
        return "TestGoFileFuzzedFunctionsByUtGoExecutor"
    }

    private object RawValuesCodes {
        const val NAN_VALUE = "NaN"
        const val POS_INF_VALUE = "+Inf"
        const val NEG_INF_VALUE = "-Inf"
        const val COMPLEX_PARTS_DELIMITER = "@"
    }

    private fun convertRawExecutionResultToExecutionResult(
        rawExecutionResult: RawExecutionResult, functionResultTypes: List<GoTypeId>, timeoutMillis: Long
    ): GoUtExecutionResult {
        if (rawExecutionResult.timeoutExceeded) {
            return GoUtTimeoutExceeded(timeoutMillis, rawExecutionResult.trace)
        }
        if (rawExecutionResult.panicMessage != null) {
            val (rawResultValue, _) = rawExecutionResult.panicMessage
            val panicValue = if (goPrimitives.map { it.simpleName }.contains(rawResultValue.type)) {
                createGoUtPrimitiveModelFromRawValue(rawResultValue as PrimitiveValue, GoTypeId(rawResultValue.type))
            } else {
                error("Only primitive panic value is currently supported")
            }
            return GoUtPanicFailure(panicValue, GoTypeId(rawResultValue.type), rawExecutionResult.trace)
        }
        if (rawExecutionResult.resultRawValues.size != functionResultTypes.size) {
            error("Function completed execution must have as many result raw values as result types.")
        }
        rawExecutionResult.resultRawValues.zip(functionResultTypes).forEach { (rawResultValue, resultType) ->
            if (rawResultValue == null) {
                if (resultType !is GoInterfaceTypeId) {
                    error("Result of function execution must have same type as function result")
                }
                return@forEach
            }
            if (!rawResultValue.checkIsEqualTypes(resultType)) {
                error("Result of function execution must have same type as function result")
            }
        }
        var executedWithNonNilErrorString = false
        val resultValues =
            rawExecutionResult.resultRawValues.zip(functionResultTypes).map { (rawResultValue, resultType) ->
                if (resultType.implementsError && rawResultValue != null) {
                    executedWithNonNilErrorString = true
                }
                createGoUtModelFromRawValue(rawResultValue, resultType)
            }
        return if (executedWithNonNilErrorString) {
            GoUtExecutionWithNonNilError(resultValues, rawExecutionResult.trace)
        } else {
            GoUtExecutionSuccess(resultValues, rawExecutionResult.trace)
        }
    }

    private fun createGoUtModelFromRawValue(rawResultValue: RawResultValue?, typeId: GoTypeId): GoUtModel {
        return when (typeId) {
            // Only for error interface
            is GoInterfaceTypeId -> if (rawResultValue == null) GoUtNilModel(typeId)
            else GoUtPrimitiveModel((rawResultValue as PrimitiveValue).value, goStringTypeId)

            is GoStructTypeId -> createGoUtStructModelFromRawValue(rawResultValue as StructValue, typeId)

            is GoArrayTypeId -> createGoUtArrayModelFromRawValue(rawResultValue as ArrayValue, typeId)

            else -> createGoUtPrimitiveModelFromRawValue(rawResultValue as PrimitiveValue, typeId)
        }
    }

    private fun createGoUtPrimitiveModelFromRawValue(
        resultValue: PrimitiveValue, typeId: GoTypeId
    ): GoUtPrimitiveModel {
        val rawValue = resultValue.value
        if (typeId == goFloat64TypeId || typeId == goFloat32TypeId) {
            return convertRawFloatValueToGoUtPrimitiveModel(rawValue, typeId)
        }
        if (typeId == goComplex128TypeId || typeId == goComplex64TypeId) {
            val correspondingFloatType = if (typeId == goComplex128TypeId) goFloat64TypeId else goFloat32TypeId
            val (realPartModel, imagPartModel) = rawValue.split(RawValuesCodes.COMPLEX_PARTS_DELIMITER).map {
                convertRawFloatValueToGoUtPrimitiveModel(it, correspondingFloatType, typeId == goComplex64TypeId)
            }
            return GoUtComplexModel(realPartModel, imagPartModel, typeId)
        }
        val value = when (typeId.correspondingKClass) {
            UByte::class -> rawValue.toUByte()
            Boolean::class -> rawValue.toBoolean()
            Float::class -> rawValue.toFloat()
            Double::class -> rawValue.toDouble()
            Int::class -> rawValue.toInt()
            Short::class -> rawValue.toShort()
            Long::class -> rawValue.toLong()
            Byte::class -> rawValue.toByte()
            UInt::class -> rawValue.toUInt()
            UShort::class -> rawValue.toUShort()
            ULong::class -> rawValue.toULong()
            else -> rawValue
        }
        return GoUtPrimitiveModel(value, typeId)
    }

    private fun convertRawFloatValueToGoUtPrimitiveModel(
        rawValue: String, typeId: GoTypeId, explicitCastRequired: Boolean = false
    ): GoUtPrimitiveModel {
        return when (rawValue) {
            RawValuesCodes.NAN_VALUE -> GoUtFloatNaNModel(typeId)
            RawValuesCodes.POS_INF_VALUE -> GoUtFloatInfModel(1, typeId)
            RawValuesCodes.NEG_INF_VALUE -> GoUtFloatInfModel(-1, typeId)
            else -> {
                val typedValue = if (typeId == goFloat64TypeId) rawValue.toDouble() else rawValue.toFloat()
                if (explicitCastRequired) {
                    GoUtPrimitiveModel(typedValue, typeId, explicitCastMode = ExplicitCastMode.REQUIRED)
                } else {
                    GoUtPrimitiveModel(typedValue, typeId)
                }
            }
        }
    }

    private fun createGoUtStructModelFromRawValue(
        resultValue: StructValue, resultTypeId: GoStructTypeId
    ): GoUtStructModel {
        val value = resultValue.value.zip(resultTypeId.fields).map { (value, fieldId) ->
            fieldId.name to createGoUtModelFromRawValue(value.value, fieldId.declaringClass as GoTypeId)
        }
        return GoUtStructModel(value, resultTypeId)
    }

    private fun createGoUtArrayModelFromRawValue(
        resultValue: ArrayValue, resultTypeId: GoArrayTypeId
    ): GoUtArrayModel {
        val value = (0 until resultTypeId.length).associateWith { index ->
            createGoUtModelFromRawValue(resultValue.value[index], resultTypeId.elementTypeId)
        }.toMutableMap()
        return GoUtArrayModel(value, resultTypeId, value.size)
    }
}