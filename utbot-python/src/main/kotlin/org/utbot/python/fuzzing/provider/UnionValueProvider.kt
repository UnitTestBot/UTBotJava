package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonUnionTypeDescription
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonAnnotationParameters
import org.utbot.python.newtyping.pythonTypeRepresentation

object UnionValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        return type.meta is PythonUnionTypeDescription
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val params = type.pythonAnnotationParameters()
        params.forEach { unionParam ->
            yield(Seed.Recursive(
                construct = Routine.Create(listOf(unionParam)) { v -> v.first() },
                empty = Routine.Empty { PythonFuzzedValue(PythonTree.FakeNode) }
            ))
        }
    }
}