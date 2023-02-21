package org.utbot.go.fuzzer.providers

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.go.GoDescription
import org.utbot.go.api.GoPrimitiveTypeId
import org.utbot.go.api.GoUtPrimitiveModel
import org.utbot.go.api.util.*
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel

object GoConstantValueProvider : ValueProvider<GoTypeId, GoUtModel, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type in goConstantTypes

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, GoUtModel>> = sequence {
        type.let { it as GoPrimitiveTypeId }.also { primitiveType ->
            val constants = description.methodUnderTest.constants
            val intSize = description.intSize
            val primitives: List<Seed<GoTypeId, GoUtModel>> = when (type) {
                goRuneTypeId, goIntTypeId, goInt8TypeId, goInt16TypeId, goInt32TypeId, goInt64TypeId ->
                    (constants[type] ?: emptyList()).map {
                        when (type) {
                            goInt8TypeId -> Seed.Known<GoTypeId, GoUtModel, BitVectorValue>(BitVectorValue.fromValue(it)) { obj: BitVectorValue ->
                                GoUtPrimitiveModel(
                                    obj.toByte(),
                                    primitiveType
                                )
                            }

                            goInt16TypeId -> Seed.Known<GoTypeId, GoUtModel, BitVectorValue>(BitVectorValue.fromValue(it)) { obj: BitVectorValue ->
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
                    }

                else -> emptyList()
            }

            primitives.forEach { seed ->
                yield(seed)
            }
        }
    }
}