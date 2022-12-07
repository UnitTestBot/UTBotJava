package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonSetClassId
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.general.Type

object SetValueProvider : ValueProvider<Type, PythonTreeModel, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        val meta = type.meta
        return (meta is PythonConcreteCompositeTypeDescription) && meta.name.toString() == "builtins.set"
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val meta = type.meta as PythonConcreteCompositeTypeDescription
        val params = meta.getAnnotationParameters(type)

        val modifications = emptyList<Routine.Call<Type, PythonTreeModel>>().toMutableList()
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
                PythonTreeModel(
                    PythonTree.SetNode(items),
                    pythonSetClassId
                )
            },
            modify = modifications.asSequence(),
            empty = Routine.Empty { PythonTreeModel(
                PythonTree.SetNode(emptySet<PythonTree.PythonTreeNode>().toMutableSet()),
                pythonSetClassId
            )}
        ))
    }
}