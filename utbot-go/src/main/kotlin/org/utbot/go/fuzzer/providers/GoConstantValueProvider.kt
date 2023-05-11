package org.utbot.go.fuzzer.providers

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.IEEE754Value
import org.utbot.fuzzing.seeds.StringValue
import org.utbot.go.GoDescription
import org.utbot.go.api.GoPrimitiveTypeId
import org.utbot.go.api.GoUtPrimitiveModel
import org.utbot.go.api.util.*
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel

object GoConstantValueProvider : ValueProvider<GoTypeId, GoUtModel, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type in goSupportedConstantTypes

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, GoUtModel>> = sequence {
        type.let { it as GoPrimitiveTypeId }.also { primitiveType ->
            val constants = description.functionUnderTest.constants
            val intSize = description.intSize
            val primitives: List<Seed<GoTypeId, GoUtModel>> = (constants[primitiveType] ?: emptyList()).mapNotNull {
                when (primitiveType) {
                    goRuneTypeId, goIntTypeId, goInt8TypeId, goInt16TypeId, goInt32TypeId, goInt64TypeId ->
                        when (primitiveType) {
                            goInt8TypeId -> Seed.Known(BitVectorValue.fromValue(it)) { obj: BitVectorValue ->
                                GoUtPrimitiveModel(
                                    obj.toByte(),
                                    primitiveType
                                )
                            }

                            goInt16TypeId -> Seed.Known(BitVectorValue.fromValue(it)) { obj: BitVectorValue ->
                                GoUtPrimitiveModel(
                                    obj.toShort(),
                                    primitiveType
                                )
                            }

                            goInt32TypeId, goRuneTypeId -> Seed.Known(BitVectorValue.fromValue(it)) { obj: BitVectorValue ->
                                GoUtPrimitiveModel(
                                    obj.toInt(),
                                    primitiveType
                                )
                            }

                            goIntTypeId -> Seed.Known(BitVectorValue.fromValue(it)) { obj: BitVectorValue ->
                                GoUtPrimitiveModel(
                                    if (intSize == 32) obj.toInt() else obj.toLong(),
                                    primitiveType
                                )
                            }

                            goInt64TypeId -> Seed.Known(BitVectorValue.fromValue(it)) { obj: BitVectorValue ->
                                GoUtPrimitiveModel(
                                    obj.toLong(),
                                    primitiveType
                                )
                            }

                            else -> return@sequence
                        }

                    goByteTypeId, goUintTypeId, goUintPtrTypeId, goUint8TypeId, goUint16TypeId, goUint32TypeId, goUint64TypeId ->
                        when (primitiveType) {
                            goByteTypeId, goUint8TypeId -> {
                                val uint8AsLong = (it as UByte).toLong()
                                Seed.Known(BitVectorValue.fromValue(uint8AsLong)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toUByte(),
                                        primitiveType
                                    )
                                }
                            }

                            goUint16TypeId -> {
                                val uint16AsLong = (it as UShort).toLong()
                                Seed.Known(BitVectorValue.fromValue(uint16AsLong)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toUShort(),
                                        primitiveType
                                    )
                                }
                            }

                            goUint32TypeId -> {
                                val uint32AsLong = (it as UInt).toLong()
                                Seed.Known(BitVectorValue.fromValue(uint32AsLong)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toUInt(),
                                        primitiveType
                                    )
                                }
                            }

                            goUintTypeId, goUintPtrTypeId -> {
                                val uintAsLong = if (intSize == 32) (it as UInt).toLong() else (it as ULong).toLong()
                                Seed.Known(BitVectorValue.fromValue(uintAsLong)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        if (intSize == 32) obj.toUInt() else obj.toULong(),
                                        primitiveType
                                    )
                                }
                            }

                            goUint64TypeId -> {
                                val uint64AsLong = (it as ULong).toLong()
                                Seed.Known(BitVectorValue.fromValue(uint64AsLong)) { obj: BitVectorValue ->
                                    GoUtPrimitiveModel(
                                        obj.toULong(),
                                        primitiveType
                                    )
                                }
                            }

                            else -> return@sequence
                        }

                    goFloat32TypeId -> Seed.Known(IEEE754Value.fromValue(it)) { obj: IEEE754Value ->
                        GoUtPrimitiveModel(
                            obj.toFloat(),
                            primitiveType
                        )
                    }

                    goFloat64TypeId -> Seed.Known(IEEE754Value.fromValue(it)) { obj: IEEE754Value ->
                        GoUtPrimitiveModel(
                            obj.toDouble(),
                            primitiveType
                        )
                    }

                    goStringTypeId -> Seed.Known(StringValue(it as String)) { obj: StringValue ->
                        GoUtPrimitiveModel(
                            obj.value,
                            primitiveType
                        )
                    }

                    else -> null
                }
            }

            primitives.forEach { seed ->
                yield(seed)
            }
        }
    }
}