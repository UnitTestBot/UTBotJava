package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonIteratorClassId
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.pythonAnnotationParameters
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.newtyping.pythonTypeRepresentation

object IteratorValueProvider : ValueProvider<UtType, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: UtType): Boolean {
        return type.pythonTypeName() == pythonIteratorClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: UtType) = sequence {
        val param = type.pythonAnnotationParameters()
        yield(
            Seed.Collection(
                construct = Routine.Collection {
                    PythonFuzzedValue(
                        PythonTree.IteratorNode(
                            emptyMap<Int, PythonTree.PythonTreeNode>().toMutableMap(),
                        ),
                        "%var% = ${type.pythonTypeRepresentation()}"
                    )
                },
                modify = Routine.ForEach(param) { self, i, values ->
                    (self.tree as PythonTree.IteratorNode).items[i] = values.first().tree
                }
            ))
    }
}