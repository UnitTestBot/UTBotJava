package org.utbot.go.fuzzer.providers

import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue
import org.utbot.go.api.*
import org.utbot.go.api.util.*

// This class is highly based on PrimitiveDefaultsModelProvider.
object GoPrimitivesModelProvider : ModelProvider {

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap.forEach { (classId, parameterIndices) ->
            val typeId = classId as? GoTypeId ?: return@forEach

            val primitives: List<FuzzedValue> = when (typeId) {
                goByteTypeId -> generateUnsignedIntegerModels(typeId, goUint8TypeId.name)

                goBoolTypeId -> listOf(
                    GoUtPrimitiveModel(false, typeId).fuzzed { summary = "%var% = false" },
                    GoUtPrimitiveModel(true, typeId).fuzzed { summary = "%var% = true" }
                )

                goComplex128TypeId, goComplex64TypeId -> generateComplexModels(typeId)

                goFloat32TypeId, goFloat64TypeId -> generateFloatModels(typeId)

                goIntTypeId, goInt16TypeId, goInt32TypeId, goInt64TypeId, goInt8TypeId ->
                    generateSignedIntegerModels(typeId)

                goRuneTypeId -> generateSignedIntegerModels(typeId, goInt32TypeId.name)

                goStringTypeId -> listOf(
                    GoUtPrimitiveModel("\"\"", typeId).fuzzed { summary = "%var% = empty string" },
                    GoUtPrimitiveModel("\"   \"", typeId).fuzzed { summary = "%var% = blank string" },
                    GoUtPrimitiveModel("\"string\"", typeId).fuzzed { summary = "%var% != empty string" },
                    GoUtPrimitiveModel("\"\\n\\t\\r\"", typeId).fuzzed { summary = "%var% has special characters" },
                )

                goUintTypeId, goUint16TypeId, goUint32TypeId, goUint64TypeId, goUint8TypeId ->
                    generateUnsignedIntegerModels(typeId)

                goUintPtrTypeId -> listOf(
                    GoUtPrimitiveModel(0, typeId).fuzzed { summary = "%var% = 0" },
                    GoUtPrimitiveModel(1, typeId).fuzzed { summary = "%var% > 0" },
                )

                else -> emptyList()
            }

            primitives.forEach { model ->
                parameterIndices.forEach { index ->
                    yieldValue(index, model)
                }
            }
        }
    }

    private fun generateSignedIntegerModels(typeId: GoTypeId, mathTypeName: String = typeId.name): List<FuzzedValue> {
        val minValue = "math.Min${mathTypeName.capitalize()}"
        val maxValue = "math.Max${mathTypeName.capitalize()}"
        return listOf(
            GoUtPrimitiveModel(0, typeId).fuzzed { summary = "%var% = 0" },
            GoUtPrimitiveModel(1, typeId).fuzzed { summary = "%var% > 0" },
            GoUtPrimitiveModel(-1, typeId).fuzzed { summary = "%var% < 0" },
            GoUtPrimitiveModel(minValue, typeId, setOf("math")).fuzzed { summary = "%var% = $minValue" },
            GoUtPrimitiveModel(maxValue, typeId, setOf("math")).fuzzed { summary = "%var% = $maxValue" },
        )
    }

    private fun generateUnsignedIntegerModels(typeId: GoTypeId, mathTypeName: String = typeId.name): List<FuzzedValue> {
        val maxValue = "math.Max${mathTypeName.capitalize()}"
        return listOf(
            GoUtPrimitiveModel(0, typeId).fuzzed { summary = "%var% = 0" },
            GoUtPrimitiveModel(1, typeId).fuzzed { summary = "%var% > 0" },
            GoUtPrimitiveModel(maxValue, typeId, setOf("math")).fuzzed { summary = "%var% = $maxValue" },
        )
    }

    private fun generateFloatModels(
        typeId: GoTypeId,
        explicitCastRequired: Boolean = false
    ): List<FuzzedValue> {
        val maxValue = "math.Max${typeId.name.capitalize()}"
        val smallestNonZeroValue = "math.SmallestNonzero${typeId.name.capitalize()}"

        val explicitCastRequiredModeIfFloat32 =
            getExplicitCastModeForFloatModel(typeId, explicitCastRequired, ExplicitCastMode.REQUIRED)
        val explicitCastMode = getExplicitCastModeForFloatModel(typeId, explicitCastRequired, ExplicitCastMode.DEPENDS)

        return listOf(
            GoUtPrimitiveModel(0.0, typeId, explicitCastMode = explicitCastMode).fuzzed {
                summary = "%var% = 0.0"
            },
            GoUtPrimitiveModel(1.1, typeId, explicitCastMode = explicitCastMode).fuzzed {
                summary = "%var% > 0.0"
            },
            GoUtPrimitiveModel(-1.1, typeId, explicitCastMode = explicitCastMode).fuzzed {
                summary = "%var% < 0.0"
            },
            GoUtPrimitiveModel(
                smallestNonZeroValue,
                typeId,
                requiredImports = setOf("math"),
                explicitCastMode = explicitCastRequiredModeIfFloat32
            ).fuzzed {
                summary = "%var% = $smallestNonZeroValue"
            },
            GoUtPrimitiveModel(
                maxValue,
                typeId,
                requiredImports = setOf("math"),
                explicitCastMode = explicitCastRequiredModeIfFloat32
            ).fuzzed {
                summary = "%var% = $maxValue"
            },
            GoUtFloatInfModel(-1, typeId).fuzzed { summary = "%var% = math.Inf(-1)" },
            GoUtFloatInfModel(1, typeId).fuzzed { summary = "%var% = math.Inf(1)" },
            GoUtFloatNaNModel(typeId).fuzzed { summary = "%var% = math.NaN()" },
        )
    }

    private fun <T> cartesianProduct(listA: List<T>, listB: List<T>): List<List<T>> {
        val result = mutableListOf<List<T>>()
        listA.forEach { a -> listB.forEach { b -> result.add(listOf(a, b)) } }
        return result
    }

    private fun generateComplexModels(typeId: GoTypeId): List<FuzzedValue> {
        val correspondingFloatType = if (typeId == goComplex128TypeId) goFloat64TypeId else goFloat32TypeId
        val componentModels = generateFloatModels(correspondingFloatType, typeId == goComplex64TypeId)
        return cartesianProduct(componentModels, componentModels).map { (realFuzzedValue, imagFuzzedValue) ->
            GoUtComplexModel(
                realFuzzedValue.model as GoUtPrimitiveModel,
                imagFuzzedValue.model as GoUtPrimitiveModel,
                typeId
            ).fuzzed { summary = "%var% = complex(${realFuzzedValue.summary}, ${imagFuzzedValue.summary})" }
        }
    }
}