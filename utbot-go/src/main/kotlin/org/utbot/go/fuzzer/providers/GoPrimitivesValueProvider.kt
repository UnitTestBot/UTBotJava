package org.utbot.go.fuzzer.providers

import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.providers.RegexModelProvider.fuzzed
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.*
import org.utbot.go.GoDescription
import org.utbot.go.api.GoPrimitiveTypeId
import org.utbot.go.api.GoUtComplexModel
import org.utbot.go.api.GoUtPrimitiveModel
import org.utbot.go.api.util.*
import org.utbot.go.framework.api.go.GoTypeId
import java.util.*

object GoPrimitivesValueProvider : ValueProvider<GoTypeId, FuzzedValue, GoDescription> {

    private val random = Random(0)

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

                                goInt64TypeId -> Seed.Known(it.invoke(64)) { obj: BitVectorValue ->
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

                    goFloat32TypeId -> generateFloat32Seeds(primitiveType)

                    goFloat64TypeId -> generateFloat64Seeds(primitiveType)

                    goComplex64TypeId -> generateComplexSeeds(primitiveType, goFloat32TypeId)

                    goComplex128TypeId -> generateComplexSeeds(primitiveType, goFloat64TypeId)

                    goStringTypeId -> listOf(
                        Seed.Known(StringValue("")) { obj: StringValue ->
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

    private fun generateFloat32Seeds(typeId: GoPrimitiveTypeId): List<Seed<GoTypeId, FuzzedValue>> {
        return listOf(
            Seed.Known(IEEE754Value.fromFloat(random.nextFloat())) { obj: IEEE754Value ->
                GoUtPrimitiveModel(obj.toFloat(), typeId).fuzzed {
                    summary = "%var% = ${obj.toFloat()}"
                }
            }
        )
    }

    private fun generateFloat64Seeds(typeId: GoPrimitiveTypeId): List<Seed<GoTypeId, FuzzedValue>> {
        return listOf(
            Seed.Known(IEEE754Value.fromDouble(random.nextDouble())) { obj: IEEE754Value ->
                GoUtPrimitiveModel(obj.toDouble(), typeId).fuzzed {
                    summary = "%var% = ${obj.toDouble()}"
                }
            }
        )
    }

    private fun generateComplexSeeds(
        typeId: GoPrimitiveTypeId,
        floatTypeId: GoPrimitiveTypeId
    ): List<Seed<GoTypeId, FuzzedValue>> {
        return listOf(
            Seed.Recursive(
                construct = Routine.Create(listOf(floatTypeId, floatTypeId)) { values ->
                    GoUtComplexModel(
                        realValue = values[0].model as GoUtPrimitiveModel,
                        imagValue = values[1].model as GoUtPrimitiveModel,
                        typeId = typeId
                    ).fuzzed {
                        summary = "%var% = $model"
                    }
                },
                modify = sequence {
                    yield(Routine.Call(listOf(floatTypeId)) { self, values ->
                        val model = self.model as GoUtComplexModel
                        val value = values.first().model as GoUtPrimitiveModel
                        model.realValue = value
                    })
                },
                empty = Routine.Empty {
                    GoUtComplexModel(
                        realValue = GoUtPrimitiveModel(0.0, floatTypeId),
                        imagValue = GoUtPrimitiveModel(0.0, floatTypeId),
                        typeId = typeId
                    ).fuzzed {
                        summary = "%var% = $model"
                    }
                }
            )
        )
    }
}
