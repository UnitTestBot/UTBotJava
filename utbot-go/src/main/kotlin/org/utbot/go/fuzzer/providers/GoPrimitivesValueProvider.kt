package org.utbot.go.fuzzer.providers

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.*
import org.utbot.go.GoDescription
import org.utbot.go.api.*
import org.utbot.go.api.util.*
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel
import java.util.*
import kotlin.math.sign

object GoPrimitivesValueProvider : ValueProvider<GoTypeId, GoUtModel, GoDescription> {
    private val random = Random(0)

    override fun accept(type: GoTypeId): Boolean = type in goPrimitives

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, GoUtModel>> =
        sequence {
            type.let { it as GoPrimitiveTypeId }.also { primitiveType ->
                val primitives: List<Seed<GoTypeId, GoUtModel>> = when (primitiveType) {
                    goBoolTypeId -> listOf(
                        Seed.Known(Bool.FALSE.invoke()) { obj: BitVectorValue ->
                            GoUtPrimitiveModel(
                                obj.toBoolean(),
                                primitiveType
                            )
                        },
                        Seed.Known(Bool.TRUE.invoke()) { obj: BitVectorValue ->
                            GoUtPrimitiveModel(
                                obj.toBoolean(),
                                primitiveType
                            )
                        }
                    )

                    goRuneTypeId, goIntTypeId, goInt8TypeId, goInt16TypeId, goInt32TypeId, goInt64TypeId -> Signed.values()
                        .map {
                            when (type) {
                                goInt8TypeId -> Seed.Known(it.invoke(8)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toByte(),
                                        primitiveType
                                    )
                                }

                                goInt16TypeId -> Seed.Known(it.invoke(16)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toShort(),
                                        primitiveType
                                    )
                                }

                                goInt32TypeId, goRuneTypeId -> Seed.Known(it.invoke(32)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toInt(),
                                        primitiveType
                                    )
                                }

                                goIntTypeId -> Seed.Known(it.invoke(intSize)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        if (intSize == 32) obj.toInt() else obj.toLong(),
                                        primitiveType
                                    )
                                }

                                goInt64TypeId -> Seed.Known(it.invoke(64)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toLong(),
                                        primitiveType
                                    )
                                }

                                else -> return@sequence
                            }
                        }

                    goByteTypeId, goUintTypeId, goUintPtrTypeId, goUint8TypeId, goUint16TypeId, goUint32TypeId, goUint64TypeId -> Unsigned.values()
                        .map {
                            when (type) {
                                goByteTypeId, goUint8TypeId -> Seed.Known(it.invoke(8)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toUByte(),
                                        primitiveType
                                    )
                                }

                                goUint16TypeId -> Seed.Known(it.invoke(16)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toUShort(),
                                        primitiveType
                                    )
                                }

                                goUint32TypeId -> Seed.Known(it.invoke(32)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toUInt(),
                                        primitiveType
                                    )
                                }

                                goUintTypeId, goUintPtrTypeId -> Seed.Known(it.invoke(intSize)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        if (intSize == 32) obj.toUInt() else obj.toULong(),
                                        primitiveType
                                    )
                                }

                                goUint64TypeId -> Seed.Known(it.invoke(64)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toULong(),
                                        primitiveType
                                    )
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
                                obj.value,
                                primitiveType
                            )
                        },
                        Seed.Known(StringValue("hello")) { obj: StringValue ->
                            GoUtPrimitiveModel(
                                obj.value,
                                primitiveType
                            )
                        })

                    else -> emptyList()
                }

                primitives.forEach { seed ->
                    yield(seed)
                }
            }
        }

    private fun generateFloat32Seeds(typeId: GoPrimitiveTypeId): List<Seed<GoTypeId, GoUtModel>> {
        return listOf(
            Seed.Known(IEEE754Value.fromFloat(random.nextFloat())) { obj: IEEE754Value ->
                val d = obj.toFloat()
                if (d.isInfinite()) {
                    GoUtFloatInfModel(d.sign.toInt(), typeId)
                } else if (d.isNaN()) {
                    GoUtFloatNaNModel(typeId)
                } else {
                    GoUtPrimitiveModel(d, typeId)
                }
            }
        )
    }

    private fun generateFloat64Seeds(typeId: GoPrimitiveTypeId): List<Seed<GoTypeId, GoUtModel>> {
        return listOf(
            Seed.Known(IEEE754Value.fromDouble(random.nextDouble())) { obj: IEEE754Value ->
                val d = obj.toDouble()
                if (d.isInfinite()) {
                    GoUtFloatInfModel(d.sign.toInt(), typeId)
                } else if (d.isNaN()) {
                    GoUtFloatNaNModel(typeId)
                } else {
                    GoUtPrimitiveModel(d, typeId)
                }
            }
        )
    }

    private fun generateComplexSeeds(
        typeId: GoPrimitiveTypeId,
        floatTypeId: GoPrimitiveTypeId
    ): List<Seed<GoTypeId, GoUtModel>> {
        return listOf(
            Seed.Recursive(
                construct = Routine.Create(listOf(floatTypeId, floatTypeId)) { values ->
                    GoUtComplexModel(
                        realValue = values[0] as GoUtPrimitiveModel,
                        imagValue = values[1] as GoUtPrimitiveModel,
                        typeId = typeId
                    )
                },
                modify = sequence {
                    yield(Routine.Call(listOf(floatTypeId)) { self, values ->
                        val model = self as GoUtComplexModel
                        val value = values.first() as GoUtPrimitiveModel
                        model.realValue = value
                    })
                },
                empty = Routine.Empty {
                    GoUtComplexModel(
                        realValue = GoUtPrimitiveModel(0.0, floatTypeId),
                        imagValue = GoUtPrimitiveModel(0.0, floatTypeId),
                        typeId = typeId
                    )
                }
            )
        )
    }
}
