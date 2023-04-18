package org.utbot.go.fuzzer.providers

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.go.GoDescription
import org.utbot.go.api.GoArrayTypeId
import org.utbot.go.api.GoUtArrayModel
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel

object GoArrayValueProvider : ValueProvider<GoTypeId, GoUtModel, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type is GoArrayTypeId

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, GoUtModel>> =
        sequence {
            type.let { it as GoArrayTypeId }.also { arrayType ->
                yield(
                    Seed.Collection(
                        construct = Routine.Collection {
                            GoUtArrayModel(
                                value = arrayOfNulls(arrayType.length),
                                typeId = arrayType,
                            )
                        },
                        modify = Routine.ForEach(listOf(arrayType.elementTypeId!!)) { self, i, values ->
                            val model = self as GoUtArrayModel
                            if (i >= model.length) {
                                return@ForEach
                            }
                            model.value[i] = values.first()
                        }
                    )
                )
            }
        }
}