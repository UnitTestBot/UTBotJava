package org.utbot.go.fuzzer.providers

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.go.GoDescription
import org.utbot.go.api.*
import org.utbot.go.framework.api.go.GoTypeId
import org.utbot.go.framework.api.go.GoUtModel

object GoNilValueProvider : ValueProvider<GoTypeId, GoUtModel, GoDescription> {
    override fun accept(type: GoTypeId): Boolean =
        type is GoSliceTypeId || type is GoMapTypeId || type is GoChanTypeId || type is GoPointerTypeId || type is GoInterfaceTypeId

    override fun generate(description: GoDescription, type: GoTypeId): Sequence<Seed<GoTypeId, GoUtModel>> =
        sequenceOf(Seed.Simple(GoUtNilModel(type)))
}