package org.utbot.go.fuzzer.providers

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.go.GoDescription
import org.utbot.go.api.GoNamedTypeId
import org.utbot.go.api.GoUtNamedModel
import org.utbot.go.api.util.goDefaultValueModel
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel

object GoNamedValueProvider : ValueProvider<GoTypeId, GoUtModel, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type is GoNamedTypeId

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, GoUtModel>> = sequence {
        type.let { it as GoNamedTypeId }.also { namedType ->
            yield(Seed.Recursive(construct = Routine.Create(listOf(namedType.underlyingTypeId)) { values ->
                GoUtNamedModel(
                    value = values.first(),
                    typeId = namedType,
                )
            }, modify = sequence {
                yield(Routine.Call(listOf(namedType.underlyingTypeId)) { self, values ->
                    val model = self as GoUtNamedModel
                    val value = values.first()
                    model.value = value
                })
            }, empty = Routine.Empty {
                namedType.goDefaultValueModel()
            }))
        }
    }
}