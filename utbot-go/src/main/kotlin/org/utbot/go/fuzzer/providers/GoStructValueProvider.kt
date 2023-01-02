package org.utbot.go.fuzzer.providers

import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.providers.CharToStringModelProvider.fuzzed
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.go.GoDescription
import org.utbot.go.api.GoStructTypeId
import org.utbot.go.api.GoTypeId
import org.utbot.go.api.GoUtStructModel
import org.utbot.go.framework.api.go.GoUtModel
import org.utbot.go.util.goRequiredImports

object GoStructValueProvider : ValueProvider<GoTypeId, FuzzedValue, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type is GoStructTypeId

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, FuzzedValue>> =
        sequence {
            type.let { it as GoStructTypeId }.also { structType ->
                structType.allConstructors.forEach { constructorId ->
                    yield(Seed.Recursive(
                        construct = Routine.Create(constructorId.parameters.map { it as GoTypeId }) { values ->
                            GoUtStructModel(
                                value = structType.fields.zip(values).map { (field, value) ->
                                    field.name to value.model as GoUtModel
                                },
                                typeId = structType,
                                requiredImports = values.goRequiredImports
                            ).fuzzed {
                                summary = "%var% = ${this.model}"
                            }
                        },
                        modify = sequence {
                            structType.fields.forEachIndexed { index, field ->
                                yield(Routine.Call(listOf(field.declaringClass as GoTypeId)) { self, values ->
                                    val model = self.model as GoUtStructModel
                                    val value = values.first().model as GoUtModel
                                    (model.value as MutableList)[index] = field.name to value
                                })
                            }
                        },
                        empty = Routine.Empty {
                            GoUtStructModel(
                                value = emptyList(),
                                typeId = structType
                            ).fuzzed {
                                summary = "%var% = ${this.model}"
                            }
                        }
                    ))
                }
            }
        }
}
