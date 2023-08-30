package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonTypeAliasDescription
import org.utbot.python.newtyping.general.UtType

object TypeAliasValueProvider : ValueProvider<UtType, PythonFuzzedValue, PythonMethodDescription> {

    override fun accept(type: UtType): Boolean {
        return type.meta is PythonTypeAliasDescription
    }

    override fun generate(description: PythonMethodDescription, type: UtType): Sequence<Seed<UtType, PythonFuzzedValue>> {
        val compositeType = PythonTypeAliasDescription.castToCompatibleTypeApi(type)
        return sequenceOf(
            Seed.Recursive(
                construct = Routine.Create(listOf(compositeType.members[0])) { v -> v.first() },
                empty = Routine.Empty { PythonFuzzedValue(PythonTree.FakeNode) }
            )
        )
    }
}