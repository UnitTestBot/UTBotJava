package org.utbot.go.fuzzer.providers

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.go.GoDescription
import org.utbot.go.api.GoArrayTypeId
import org.utbot.go.api.GoUtArrayModel
import org.utbot.go.api.util.goDefaultValueModel
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel

object GoArrayValueProvider : ValueProvider<GoTypeId, GoUtModel, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type is GoArrayTypeId

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, GoUtModel>> =
        sequence {
            type.let { it as GoArrayTypeId }.also { arrayType ->
                val elementType = arrayType.elementTypeId!!
                yield(
                    Seed.Recursive(
                        construct = Routine.Create((0 until arrayType.length).map { elementType }) { values ->
                            GoUtArrayModel(
                                value = values.toTypedArray(),
                                typeId = arrayType,
                            )
                        },
                        modify = sequence {
                            val probShuffle = description.configuration.probCollectionShuffleInsteadResultMutation
                            val numberOfShuffles = if (probShuffle != 100) {
                                arrayType.length * probShuffle / (100 - probShuffle)
                            } else {
                                1
                            }
                            if (probShuffle != 100) {
                                (0 until arrayType.length).forEach { index ->
                                    yield(Routine.Call(listOf(elementType)) { self, values ->
                                        val model = self as GoUtArrayModel
                                        val value = values.first()
                                        model.value[index] = value
                                    })
                                }
                            }
                            repeat(numberOfShuffles) {
                                yield(Routine.Call(emptyList()) { self, _ ->
                                    val model = self as GoUtArrayModel
                                    model.value.shuffle()
                                })
                            }
                        },
                        empty = Routine.Empty {
                            arrayType.goDefaultValueModel()
                        }
                    )
                )
            }
        }
}