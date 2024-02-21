package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.FuzzedUtType
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.PythonValueProvider
import org.utpython.types.PythonNoneTypeDescription

object NoneValueProvider : PythonValueProvider {
    override fun accept(type: FuzzedUtType): Boolean {
        return type.utType.meta is PythonNoneTypeDescription
    }

    override fun generate(description: PythonMethodDescription, type: FuzzedUtType): Sequence<Seed<FuzzedUtType, PythonFuzzedValue>> = sequence {
        yield(Seed.Simple(PythonFuzzedValue(PythonTree.fromNone(), "%var% = None")))
    }
}