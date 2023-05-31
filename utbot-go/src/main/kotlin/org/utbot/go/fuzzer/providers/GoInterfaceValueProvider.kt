package org.utbot.go.fuzzer.providers

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.go.GoDescription
import org.utbot.go.api.GoInterfaceTypeId
import org.utbot.go.api.GoUtNilModel
import org.utbot.go.api.util.goDefaultValueModel
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel

object GoInterfaceValueProvider : ValueProvider<GoTypeId, GoUtModel, GoDescription> {
    override fun accept(type: GoTypeId): Boolean = type is GoInterfaceTypeId

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, GoUtModel>> = sequence {
        type.let { it as GoInterfaceTypeId }.also { interfaceTypeId ->
            interfaceTypeId.implementations.forEach {
                yield(Seed.Recursive(
                    construct = Routine.Create(listOf(it)) { values ->
                        values.first()
                    },
                    empty = Routine.Empty {
                        interfaceTypeId.goDefaultValueModel()
                    }
                ))
            }
        }
    }
}