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
import org.utbot.python.newtyping.PythonNoneTypeDescription
import org.utbot.python.newtyping.PythonUnionTypeDescription
import org.utbot.python.newtyping.pythonAnnotationParameters

object OptionalValueProvider : PythonValueProvider {
    override fun accept(type: FuzzedUtType): Boolean {
        return type.utType.meta is PythonUnionTypeDescription && type.utType.parameters.any { it.meta is PythonNoneTypeDescription }
    }

    override fun generate(description: PythonMethodDescription, type: FuzzedUtType) = sequence {
        val params = type.utType.pythonAnnotationParameters()
        params.forEach { unionParam ->
            yield(Seed.Recursive(
                construct = Routine.Create(listOf(unionParam).toFuzzed().activateAnyIf(type)) { v -> v.first() },
                empty = Routine.Empty { PythonFuzzedValue(PythonTree.fromNone()) }
            ))
        }
    }
}