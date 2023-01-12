package org.utbot.go.fuzzer.providers

import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.providers.RegexModelProvider.fuzzed
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.*
import org.utbot.go.GoDescription
import org.utbot.go.api.*
import org.utbot.go.api.util.*
import org.utbot.go.framework.api.go.GoTypeId

object GoPrimitivesValueProvider : ValueProvider<GoTypeId, FuzzedValue, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type in goPrimitives

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, FuzzedValue>> =
        sequence {
            type.let { it as GoPrimitiveTypeId }.also { primitiveType ->
                val primitives: List<Seed<GoTypeId, FuzzedValue>> = when (primitiveType) {
                    goBoolTypeId -> listOf(
                        Seed.Known(Bool.FALSE.invoke()) { obj: BitVectorValue ->
                            GoUtPrimitiveModel(
                                obj.toBoolean(),
                                primitiveType
                            ).fuzzed { "%var% = ${obj.toBoolean()}" }
                        },
                        Seed.Known(Bool.TRUE.invoke()) { obj: BitVectorValue ->
                            GoUtPrimitiveModel(
                                obj.toBoolean(),
                                primitiveType
                            ).fuzzed { "%var% = ${obj.toBoolean()}" }
                        }
                    )


                    goRuneTypeId, goIntTypeId, goInt8TypeId, goInt16TypeId, goInt32TypeId, goInt64TypeId -> Signed.values()
                        .map {
                            when (type) {
                                goRuneTypeId, goIntTypeId, goInt32TypeId -> Seed.Known(it.invoke(32)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toInt(),
                                        primitiveType
                                    ).fuzzed { "%var% = ${obj.toInt()}" }
                                }

                                goInt8TypeId -> Seed.Known(it.invoke(8)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toByte(),
                                        primitiveType
                                    ).fuzzed { "%var% = ${obj.toByte()}" }
                                }

                                goInt16TypeId -> Seed.Known(it.invoke(16)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toShort(),
                                        primitiveType
                                    ).fuzzed { "%var% = ${obj.toShort()}" }
                                }

                                goInt64TypeId -> Seed.Known(it.invoke(32)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toLong(),
                                        primitiveType
                                    ).fuzzed { "%var% = ${obj.toLong()}" }
                                }

                                else -> return@sequence
                            }
                        }

                    goByteTypeId, goUintTypeId, goUint8TypeId, goUint16TypeId, goUint32TypeId, goUint64TypeId -> Unsigned.values()
                        .map {
                            when (type) {
                                goByteTypeId, goUint8TypeId -> Seed.Known(it.invoke(8)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toUByte(),
                                        primitiveType
                                    ).fuzzed { "%var% = ${obj.toUByte()}" }
                                }

                                goUint16TypeId -> Seed.Known(it.invoke(16)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toUShort(),
                                        primitiveType
                                    ).fuzzed { "%var% = ${obj.toUShort()}" }
                                }

                                goUintTypeId, goUint32TypeId -> Seed.Known(it.invoke(32)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toUInt(),
                                        primitiveType
                                    ).fuzzed { "%var% = ${obj.toUShort()}" }
                                }

                                goUint64TypeId -> Seed.Known(it.invoke(64)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toULong(),
                                        primitiveType
                                    ).fuzzed { "%var% = ${obj.toULong()}" }
                                }

                                else -> return@sequence
                            }
                        }

                    goFloat32TypeId, goFloat64TypeId -> generateFloatModels(primitiveType).map { Seed.Simple(it) }

                    goComplex64TypeId, goComplex128TypeId -> generateComplexModels(primitiveType).map { Seed.Simple(it) }

                    goStringTypeId -> listOf(
                        Seed.Known(StringValue("")) { obj: StringValue ->
                            GoUtPrimitiveModel(
                                "\"${obj.value}\"",
                                primitiveType
                            ).fuzzed { summary = "%var% = ${obj.value}" }
                        },
                        Seed.Known(StringValue("   ")) { obj: StringValue ->
                            GoUtPrimitiveModel(
                                "\"${obj.value}\"",
                                primitiveType
                            ).fuzzed { summary = "%var% = ${obj.value}" }
                        },
                        Seed.Known(StringValue("hello")) { obj: StringValue ->
                            GoUtPrimitiveModel(
                                "\"${obj.value}\"",
                                primitiveType
                            ).fuzzed { summary = "%var% = ${obj.value}" }
                        })

                    goUintPtrTypeId -> listOf(
                        GoUtPrimitiveModel(0, primitiveType).fuzzed { summary = "%var% = 0" },
                        GoUtPrimitiveModel(1, primitiveType).fuzzed { summary = "%var% > 0" },
                    ).map { Seed.Simple(it) }

                    else -> emptyList()
                }

                primitives.forEach { fuzzedValue ->
                    yield(fuzzedValue)
                }
            }
        }

    private fun generateFloatModels(
        typeId: GoPrimitiveTypeId,
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

    private fun generateComplexModels(typeId: GoPrimitiveTypeId): List<FuzzedValue> {
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
