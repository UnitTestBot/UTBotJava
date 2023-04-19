package org.utbot.go.fuzzer.providers

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.go.GoDescription
import org.utbot.go.api.GoPointerTypeId
import org.utbot.go.api.GoUtNilModel
import org.utbot.go.api.GoUtPointerModel
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel

object GoPointerValueProvider : ValueProvider<GoTypeId, GoUtModel, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type is GoPointerTypeId

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, GoUtModel>> = sequence {
        type.let { it as GoPointerTypeId }.also { pointerType ->
            yield(
                Seed.Recursive(
                    construct = Routine.Create(listOf(pointerType.elementTypeId!!)) { values ->
                        GoUtPointerModel(value = values.first(), typeId = pointerType)
                    },
                    modify = sequenceOf(Routine.Call(listOf(pointerType.elementTypeId)) { self, values ->
                        val model = self as GoUtPointerModel
                        val value = values.first()
                        model.value = value
                    }),
                    empty = Routine.Empty { GoUtNilModel(pointerType) }
                )
            )
        }
    }
}