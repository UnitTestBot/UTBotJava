package org.utbot.go.fuzzer.providers

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.go.GoDescription
import org.utbot.go.api.GoStructTypeId
import org.utbot.go.api.GoUtStructModel
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtFieldModel
import org.utbot.go.framework.api.go.GoUtModel

object GoStructValueProvider : ValueProvider<GoTypeId, GoUtModel, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type is GoStructTypeId

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, GoUtModel>> =
        sequence {
            type.let { it as GoStructTypeId }.also { structType ->
                val packageName = description.methodUnderTest.getPackageName()
                val fields = structType.fields
                    .filter { structType.packageName == packageName || it.isExported }
                yield(Seed.Recursive(
                    construct = Routine.Create(fields.map { it.declaringType }) { values ->
                        GoUtStructModel(
                            value = fields.zip(values).map { (field, value) ->
                                GoUtFieldModel(value, field)
                            },
                            typeId = structType,
                            packageName = packageName,
                        )
                    },
                    modify = sequence {
                        fields.forEachIndexed { index, field ->
                            yield(Routine.Call(listOf(field.declaringType)) { self, values ->
                                val model = self as GoUtStructModel
                                val value = values.first()
                                (model.value as MutableList)[index] = GoUtFieldModel(value, field)
                            })
                        }
                    },
                    empty = Routine.Empty {
                        GoUtStructModel(
                            value = emptyList(),
                            typeId = structType,
                            packageName = packageName
                        )
                    }
                ))
            }
        }
}
