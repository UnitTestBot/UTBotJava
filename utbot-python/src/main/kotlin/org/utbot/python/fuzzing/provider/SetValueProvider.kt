package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonSetClassId
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.pythonAnnotationParameters
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.newtyping.pythonTypeRepresentation

object SetValueProvider : ValueProvider<UtType, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: UtType): Boolean {
        return type.pythonTypeName() == pythonSetClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: UtType) = sequence {
        val params = type.pythonAnnotationParameters()

        val modifications = emptyList<Routine.Call<UtType, PythonFuzzedValue>>().toMutableList()
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
            construct = Routine.Create(emptyList()) {
                val items = emptySet<PythonTree.PythonTreeNode>().toMutableSet()
                PythonFuzzedValue(
                    PythonTree.SetNode(items),
                    "%var% = ${type.pythonTypeRepresentation()}",
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