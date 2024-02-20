package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.FuzzedUtType
import org.utbot.python.fuzzing.FuzzedUtType.Companion.activateAnyIf
import org.utbot.python.fuzzing.FuzzedUtType.Companion.toFuzzed
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.PythonValueProvider
import org.utpython.types.PythonTypeAliasDescription

object TypeAliasValueProvider : PythonValueProvider {
    override fun accept(type: FuzzedUtType): Boolean {
        return type.utType.meta is PythonTypeAliasDescription
    }

    override fun generate(
        description: PythonMethodDescription,
        type: FuzzedUtType
    ): Sequence<Seed<FuzzedUtType, PythonFuzzedValue>> {
        val compositeType = PythonTypeAliasDescription.castToCompatibleTypeApi(type.utType)
        return sequenceOf(
            Seed.Recursive(
                construct = Routine.Create(listOf(compositeType.members[0]).toFuzzed().activateAnyIf(type)) { v -> v.first() },
                empty = Routine.Empty { PythonFuzzedValue(PythonTree.FakeNode) }
            )
        )
    }
}