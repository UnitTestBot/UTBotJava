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
import org.utbot.python.newtyping.PythonTupleTypeDescription
import org.utbot.python.newtyping.pythonAnnotationParameters

object TupleFixSizeValueProvider : PythonValueProvider {
    override fun accept(type: FuzzedUtType): Boolean {
        return type.utType.meta is PythonTupleTypeDescription
    }

    override fun generate(description: PythonMethodDescription, type: FuzzedUtType) = sequence {
        val params = type.utType.pythonAnnotationParameters()
        val length = params.size
        val modifications = emptyList<Routine.Call<FuzzedUtType, PythonFuzzedValue>>().toMutableList()
        for (i in 0 until length) {
            modifications.add(Routine.Call(listOf(params[i]).toFuzzed().activateAnyIf(type)) { instance, arguments ->
                (instance.tree as PythonTree.TupleNode).items[i] = arguments.first().tree
            })
        }
        yield(Seed.Recursive(
            construct = Routine.Create(params.toFuzzed().activateAnyIf(type)) { v ->
                PythonFuzzedValue(
                    PythonTree.TupleNode(v.withIndex().associate { it.index to it.value.tree }.toMutableMap()),
                    "%var% = ${type.pythonTypeRepresentation()}"
                )
            },
            modify = modifications.asSequence(),
            empty =  Routine.Empty { PythonFuzzedValue(PythonTree.FakeNode) }
        ))
    }
}