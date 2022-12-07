package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonDictClassId
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.general.Type

object DictValueProvider : ValueProvider<Type, PythonTreeModel, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        val meta = type.meta
        return (meta is PythonConcreteCompositeTypeDescription) && meta.name.toString() == "builtins.dict"
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val meta = type.meta as PythonConcreteCompositeTypeDescription
        val params = meta.getAnnotationParameters(type)

        val modifications = emptyList<Routine.Call<Type, PythonTreeModel>>().toMutableList()
        modifications.add(Routine.Call(params) { instance, arguments ->
            val key = arguments[0].tree
            val value = arguments[1].tree
            val dict = instance.tree as PythonTree.DictNode
            if (dict.items.keys.toList().contains(key)) {
                dict.items.replace(key, value)
            } else {
                dict.items[key] = value
            }
        })
        modifications.add(Routine.Call(listOf(params[0])) { instance, arguments ->
            val key = arguments[0].tree
            val dict = instance.tree as PythonTree.DictNode
            if (dict.items.keys.toList().contains(key)) {
                dict.items.remove(key)
            }
        })
        yield(Seed.Recursive(
            construct = Routine.Create(params) { v ->
                val items = mapOf(v[0].tree to v[1].tree).toMutableMap()
                PythonTreeModel(
                    PythonTree.DictNode(items),
                    pythonDictClassId
                )
            },
            modify = modifications.asSequence(),
            empty = Routine.Empty { PythonTreeModel(
                PythonTree.DictNode(emptyMap<PythonTree.PythonTreeNode, PythonTree.PythonTreeNode>().toMutableMap()),
                pythonDictClassId
            )}
        ))
    }
}