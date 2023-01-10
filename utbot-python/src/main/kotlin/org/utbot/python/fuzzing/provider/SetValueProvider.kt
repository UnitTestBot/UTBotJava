package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonAnnotationParameters
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.newtyping.pythonTypeRepresentation

object SetValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        return type.pythonTypeName() == "builtins.set"
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val params = type.pythonAnnotationParameters()

        val modifications = emptyList<Routine.Call<Type, PythonFuzzedValue>>().toMutableList()
        modifications.add(Routine.Call(params) { instance, arguments ->
            val set = instance.tree as PythonTree.SetNode
            set.items.add(arguments.first().tree)
        })
        modifications.add(Routine.Call(params) { instance, arguments ->
            val set = instance.tree as PythonTree.SetNode
            val value = arguments.first().tree
            if (set.items.contains(value)) {
                set.items.remove(value)
            }
        })
        yield(Seed.Recursive(
            construct = Routine.Create(emptyList()) { _ ->
                val items = emptySet<PythonTree.PythonTreeNode>().toMutableSet()
                PythonFuzzedValue(
                    PythonTree.SetNode(items),
                )
            },
            modify = modifications.asSequence(),
            empty = Routine.Empty { PythonFuzzedValue(
                PythonTree.SetNode(emptySet<PythonTree.PythonTreeNode>().toMutableSet()),
                "%var% = ${type.pythonTypeRepresentation()}",
            )}
        ))
    }
}