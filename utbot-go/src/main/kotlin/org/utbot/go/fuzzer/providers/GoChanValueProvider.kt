package org.utbot.go.fuzzer.providers

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.go.GoDescription
import org.utbot.go.api.GoChanTypeId
import org.utbot.go.api.GoUtChanModel
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel

object GoChanValueProvider : ValueProvider<GoTypeId, GoUtModel, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type is GoChanTypeId

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, GoUtModel>> =
        sequence {
            type.let { it as GoChanTypeId }.also { chanType ->
                yield(
                    Seed.Collection(
                        construct = Routine.Collection {
                            GoUtChanModel(
                                value = arrayOfNulls(it),
                                typeId = chanType,
                            )
                        },
                        modify = Routine.ForEach(listOf(chanType.elementTypeId!!)) { self, i, values ->
                            val model = self as GoUtChanModel
                            if (i >= model.value.size) {
                                return@ForEach
                            }
                            model.value[i] = values.first()
                        }
                    )
                )
            }
        }
}