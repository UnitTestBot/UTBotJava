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

        yield(Seed.Collection(
            construct = Routine.Collection { _ ->
                PythonFuzzedValue(
                    PythonTree.SetNode(mutableSetOf()),
                    "%var% = ${type.pythonTypeRepresentation()}"
                )
            },
            modify = Routine.ForEach(params) { instance, _, arguments ->
                val item = arguments[0].tree
                val set = instance.tree as PythonTree.SetNode
                set.items.add(item)
            },
        ))
    }
}