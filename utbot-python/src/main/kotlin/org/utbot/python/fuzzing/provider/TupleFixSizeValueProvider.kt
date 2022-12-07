package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonTupleClassId
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonTupleTypeDescription
import org.utbot.python.newtyping.general.Type

object TupleFixSizeValueProvider : ValueProvider<Type, PythonTreeModel, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        return type.meta is PythonTupleTypeDescription
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val meta = type.meta as PythonTupleTypeDescription
        val params = meta.getAnnotationParameters(type)
        val length = params.size
        val modifications = emptyList<Routine.Call<Type, PythonTreeModel>>().toMutableList()
        for (i in 0 until length) {
            modifications.add(Routine.Call(listOf(params[i])) { instance, arguments ->
                (instance.tree as PythonTree.TupleNode).items[i] = arguments.first().tree
            })
        }
        yield(Seed.Recursive(
            construct = Routine.Create(params) { v ->
                PythonTreeModel(
                    PythonTree.TupleNode(v.withIndex().associate { it.index to it.value.tree }.toMutableMap()),
                    pythonTupleClassId
                )
            },
            modify = modifications.asSequence(),
            empty = Routine.Empty { PythonTreeModel(
                PythonTree.TupleNode(emptyMap<Int, PythonTree.PythonTreeNode>().toMutableMap()),
                pythonTupleClassId
            )}
        ))
    }
}