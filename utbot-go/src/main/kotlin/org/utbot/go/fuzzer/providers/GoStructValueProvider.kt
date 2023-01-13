package org.utbot.go.fuzzer.providers

import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.providers.CharToStringModelProvider.fuzzed
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.go.GoDescription
import org.utbot.go.api.GoStructTypeId
import org.utbot.go.api.GoUtStructModel
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtFieldModel
import org.utbot.go.framework.api.go.GoUtModel

object GoStructValueProvider : ValueProvider<GoTypeId, FuzzedValue, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type is GoStructTypeId

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, FuzzedValue>> =
        sequence {
            type.let { it as GoStructTypeId }.also { structType ->
                val packageName = description.methodUnderTest.getPackageName()
                val fields = structType.fields
                    .filter { structType.packageName == packageName || it.isExported }
                yield(Seed.Recursive(
                    construct = Routine.Create(fields.map { it.declaringClass as GoTypeId }) { values ->
                        GoUtStructModel(
                            value = fields.zip(values).map { (field, value) ->
                                GoUtFieldModel(value.model as GoUtModel, field)
                            },
                            typeId = structType,
                            packageName = packageName,
                        ).fuzzed {
                            summary = "%var% = $model"
                        }
                    },
                    modify = sequence {
                        fields.forEachIndexed { index, field ->
                            yield(Routine.Call(listOf(field.declaringClass as GoTypeId)) { self, values ->
                                val model = self.model as GoUtStructModel
                                val value = values.first().model as GoUtModel
                                (model.value as MutableList)[index] = GoUtFieldModel(value, field)
                            })
                        }
                    },
                    empty = Routine.Empty {
                        GoUtStructModel(
                            value = emptyList(),
                            typeId = structType,
                            packageName = packageName
                        ).fuzzed {
                            summary = "%var% = $model"
                        }
                    }
                ))
            }
        }
}
