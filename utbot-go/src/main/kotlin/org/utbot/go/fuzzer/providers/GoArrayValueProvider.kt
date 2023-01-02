package org.utbot.go.fuzzer.providers

import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.providers.RegexModelProvider.fuzzed
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.go.GoDescription
import org.utbot.go.api.GoArrayTypeId
import org.utbot.go.api.GoTypeId
import org.utbot.go.api.GoUtArrayModel
import org.utbot.go.framework.api.go.GoUtModel

object GoArrayValueProvider : ValueProvider<GoTypeId, FuzzedValue, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type is GoArrayTypeId

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, FuzzedValue>> =
        sequence {
            type.let { it as GoArrayTypeId }.also { arrayType ->
                yield(
                    Seed.Collection(
                        construct = Routine.Collection {
                            GoUtArrayModel(
                                value = hashMapOf(),
                                typeId = arrayType,
                                length = arrayType.length
                            ).fuzzed {
                                summary = "%var% = ${this.model}"
                            }
                        },
                        modify = Routine.ForEach(listOf(arrayType.elementTypeId)) { self, i, values ->
                            if (i >= arrayType.length) {
                                return@ForEach
                            }
                            val model = self.model as GoUtArrayModel
                            model.value[i] = values.first().model as GoUtModel
                        }
                    )
                )
            }
        }
}