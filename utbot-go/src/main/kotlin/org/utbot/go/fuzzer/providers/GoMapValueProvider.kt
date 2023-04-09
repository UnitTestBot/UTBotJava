package org.utbot.go.fuzzer.providers

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.go.GoDescription
import org.utbot.go.api.GoMapTypeId
import org.utbot.go.api.GoUtMapModel
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel

object GoMapValueProvider : ValueProvider<GoTypeId, GoUtModel, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type is GoMapTypeId

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, GoUtModel>> =
        sequence {
            type.let { it as GoMapTypeId }.also { mapType ->
                yield(
                    Seed.Collection(
                        construct = Routine.Collection {
                            GoUtMapModel(
                                value = mutableMapOf(),
                                typeId = mapType
                            )
                        },
                        modify = Routine.ForEach(
                            listOf(
                                mapType.keyTypeId,
                                mapType.elementTypeId!!
                            )
                        ) { self, _, values ->
                            val model = self as GoUtMapModel
                            model.value[values[0]] = values[1]
                        }
                    )
                )
            }
        }
}