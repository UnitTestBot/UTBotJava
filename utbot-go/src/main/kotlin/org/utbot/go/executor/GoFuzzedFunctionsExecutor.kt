package org.utbot.go.executor

import org.utbot.go.api.*
import org.utbot.go.api.util.*
import org.utbot.go.util.executeCommandByNewProcessOrFail
import org.utbot.go.util.parseFromJsonOrFail
import java.io.File

object GoFuzzedFunctionsExecutor {

    fun executeGoSourceFileFuzzedFunctions(
        sourceFile: GoUtFile,
        fuzzedFunctions: List<GoUtFuzzedFunction>,
        goExecutableAbsolutePath: String
    ): Map<GoUtFuzzedFunction, GoUtExecutionResult> {
        val fileToExecuteName = createFileToExecuteName(sourceFile)
        val rawExecutionResultsFileName = createRawExecutionResultsFileName(sourceFile)

        val sourceFileDir = File(sourceFile.absoluteDirectoryPath)
        val fileToExecute = sourceFileDir.resolve(fileToExecuteName)
        val rawExecutionResultsFile = sourceFileDir.resolve(rawExecutionResultsFileName)

        val executorTestFunctionName = createExecutorTestFunctionName()
        val runGeneratedGoExecutorTestCommand = listOf(
            goExecutableAbsolutePath,
            "test",
            "-run",
            executorTestFunctionName
        )

        try {
            val fileToExecuteGoCode = GoFuzzedFunctionsExecutorCodeGenerationHelper.generateExecutorTestFileGoCode(
                sourceFile,
                fuzzedFunctions,
                executorTestFunctionName,
                rawExecutionResultsFileName
            )
            fileToExecute.writeText(fileToExecuteGoCode)

            executeCommandByNewProcessOrFail(
                runGeneratedGoExecutorTestCommand,
                sourceFileDir,
                "functions from $sourceFile"
            )
            val rawExecutionResults = parseFromJsonOrFail<RawExecutionResults>(rawExecutionResultsFile)

            return fuzzedFunctions.zip(rawExecutionResults.results)
                .associate { (fuzzedFunction, rawExecutionResult) ->
                    val executionResult = convertRawExecutionResultToExecutionResult(
                        rawExecutionResult,
                        fuzzedFunction.function.resultTypes
                    )
                    fuzzedFunction to executionResult
                }
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
        rawExecutionResult: RawExecutionResult,
        functionResultTypes: List<GoTypeId>
    ): GoUtExecutionResult {
        if (rawExecutionResult.panicMessage != null) {
            val (rawValue, rawGoType, implementsError) = rawExecutionResult.panicMessage
            if (rawValue == null) {
                return GoUtPanicFailure(GoUtNilModel(goAnyTypeId), goAnyTypeId)
            }
            val panicValueSourceGoType = GoTypeId(rawGoType, implementsError = implementsError)
            val panicValue = if (panicValueSourceGoType.isPrimitiveGoType) {
                createGoUtPrimitiveModelFromRawValue(rawValue, panicValueSourceGoType)
            } else {
                GoUtPrimitiveModel(rawValue, goStringTypeId)
            }
            return GoUtPanicFailure(panicValue, panicValueSourceGoType)
        }

        if (rawExecutionResult.resultRawValues.size != functionResultTypes.size) {
            error("Function completed execution must have as many result raw values as result types.")
        }
        var executedWithNonNilErrorString = false
        val resultValues =
            rawExecutionResult.resultRawValues.zip(functionResultTypes).map { (resultRawValue, resultType) ->
                if (resultType.implementsError && resultRawValue != null) {
                    executedWithNonNilErrorString = true
                }
                if (resultRawValue == null) {
                    GoUtNilModel(resultType)
                } else {
                    // TODO: support errors fairly, i. e. as structs; for now consider them as strings
                    val nonNilModelTypeId = if (resultType.implementsError) goStringTypeId else resultType
                    createGoUtPrimitiveModelFromRawValue(resultRawValue, nonNilModelTypeId)
                }
            }
        return if (executedWithNonNilErrorString) {
            GoUtExecutionWithNonNilError(resultValues)
        } else {
            GoUtExecutionSuccess(resultValues)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun createGoUtPrimitiveModelFromRawValue(rawValue: String, typeId: GoTypeId): GoUtPrimitiveModel {
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
        rawValue: String,
        typeId: GoTypeId,
        explicitCastRequired: Boolean = false
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
}