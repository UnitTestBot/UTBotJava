package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonSetClassId
import org.utbot.python.fuzzing.FuzzedUtType
import org.utbot.python.fuzzing.FuzzedUtType.Companion.activateAnyIf
import org.utbot.python.fuzzing.FuzzedUtType.Companion.toFuzzed
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.PythonValueProvider
import org.utpython.types.pythonAnnotationParameters

object SetValueProvider : PythonValueProvider {
    override fun accept(type: FuzzedUtType): Boolean {
        return type.pythonTypeName() == pythonSetClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: FuzzedUtType) = sequence {
        val params = type.utType.pythonAnnotationParameters()

        yield(Seed.Collection(
            construct = Routine.Collection { _ ->
                PythonFuzzedValue(
                    PythonTree.SetNode(mutableSetOf()),
                    "%var% = ${type.pythonTypeRepresentation()}"
                )
            },
            modify = Routine.ForEach(params.toFuzzed().activateAnyIf(type)) { instance, _, arguments ->
                val item = arguments[0].tree
                val set = instance.tree as PythonTree.SetNode
                set.items.add(item)
            },
        ))
    }
}