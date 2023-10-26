package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonDictClassId
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.pythonAnnotationParameters
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.newtyping.pythonTypeRepresentation

object DictValueProvider : ValueProvider<UtType, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: UtType): Boolean {
        return type.pythonTypeName() == pythonDictClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: UtType) = sequence {
        val params = type.pythonAnnotationParameters()

        val modifications = emptyList<Routine.Call<UtType, PythonFuzzedValue>>().toMutableList()
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
            construct = Routine.Create(emptyList()) { v ->
                PythonFuzzedValue(
                    PythonTree.DictNode(mutableMapOf()),
                    "%var% = ${type.pythonTypeRepresentation()}"
                )
            },
            modify = modifications.asSequence(),
            empty = Routine.Empty { PythonFuzzedValue(
                PythonTree.DictNode(emptyMap<PythonTree.PythonTreeNode, PythonTree.PythonTreeNode>().toMutableMap()),
                "%var% = ${type.pythonTypeRepresentation()}"
            )}
        ))
    }
}