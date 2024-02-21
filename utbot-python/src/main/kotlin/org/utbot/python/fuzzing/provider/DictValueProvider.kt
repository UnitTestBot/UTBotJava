package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonDictClassId
import org.utbot.python.fuzzing.FuzzedUtType
import org.utbot.python.fuzzing.FuzzedUtType.Companion.activateAnyIf
import org.utbot.python.fuzzing.FuzzedUtType.Companion.toFuzzed
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.PythonValueProvider
import org.utpython.types.pythonAnnotationParameters

object DictValueProvider : PythonValueProvider {
    override fun accept(type: FuzzedUtType): Boolean {
        return type.pythonTypeName() == pythonDictClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: FuzzedUtType) = sequence {
        val params = type.utType.pythonAnnotationParameters()

        yield(Seed.Collection(
            construct = Routine.Collection { _ ->
                PythonFuzzedValue(
                    PythonTree.DictNode(mutableMapOf()),
                    "%var% = ${type.pythonTypeRepresentation()}"
                )
            },
            modify = Routine.ForEach(params.toFuzzed().activateAnyIf(type)) { instance, _, arguments ->
                val key = arguments[0].tree
                val value = arguments[1].tree
                val dict = instance.tree as PythonTree.DictNode
                if (dict.items.keys.toList().contains(key)) {
                    dict.items.replace(key, value)
                } else {
                    dict.items[key] = value
                }
            },
        ))
    }
}