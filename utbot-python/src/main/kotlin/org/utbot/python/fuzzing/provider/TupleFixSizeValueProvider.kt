package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonTupleTypeDescription
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.pythonAnnotationParameters
import org.utbot.python.newtyping.pythonTypeRepresentation

object TupleFixSizeValueProvider : ValueProvider<UtType, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: UtType): Boolean {
        return type.meta is PythonTupleTypeDescription
    }

    override fun generate(description: PythonMethodDescription, type: UtType) = sequence {
        val params = type.pythonAnnotationParameters()
        val length = params.size
        val modifications = emptyList<Routine.Call<UtType, PythonFuzzedValue>>().toMutableList()
        for (i in 0 until length) {
            modifications.add(Routine.Call(listOf(params[i])) { instance, arguments ->
                (instance.tree as PythonTree.TupleNode).items[i] = arguments.first().tree
            })
        }
        yield(Seed.Recursive(
            construct = Routine.Create(params) { v ->
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