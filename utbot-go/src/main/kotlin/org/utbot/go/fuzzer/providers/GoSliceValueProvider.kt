package org.utbot.go.fuzzer.providers

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.go.GoDescription
import org.utbot.go.api.GoSliceTypeId
import org.utbot.go.api.GoUtSliceModel
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel

object GoSliceValueProvider : ValueProvider<GoTypeId, GoUtModel, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type is GoSliceTypeId

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, GoUtModel>> =
        sequence {
            type.let { it as GoSliceTypeId }.also { sliceType ->
                yield(
                    Seed.Collection(
                        construct = Routine.Collection {
                            GoUtSliceModel(
                                value = hashMapOf(),
                                typeId = sliceType,
                                length = it,
                            )
                        },
                        modify = Routine.ForEach(listOf(sliceType.elementTypeId!!)) { self, i, values ->
                            val model = self as GoUtSliceModel
                            model.value[i] = values.first()
                        }
                    )
                )
            }
        }
}