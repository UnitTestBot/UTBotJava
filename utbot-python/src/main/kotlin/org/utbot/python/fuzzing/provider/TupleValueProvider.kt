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

object TupleValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        return type.pythonTypeName() == "builtins.tuple"
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val param = type.pythonAnnotationParameters()
        yield(
            Seed.Collection(
                construct = Routine.Collection {
                    PythonFuzzedValue(
                        PythonTree.TupleNode(
                            emptyMap<Int, PythonTree.PythonTreeNode>().toMutableMap(),
                        ),
                        "%var% = ${type.pythonTypeRepresentation()}"
                    )
                },
                modify = Routine.ForEach(param) { self, i, values ->
                    (self.tree as PythonTree.TupleNode).items[i] = values.first().tree
                }
            ))
    }
}