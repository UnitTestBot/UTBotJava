package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonIteratorClassId
import org.utbot.python.fuzzing.FuzzedUtType
import org.utbot.python.fuzzing.FuzzedUtType.Companion.activateAnyIf
import org.utbot.python.fuzzing.FuzzedUtType.Companion.toFuzzed
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.PythonValueProvider
import org.utpython.types.pythonAnnotationParameters

object IteratorValueProvider : PythonValueProvider {
    override fun accept(type: FuzzedUtType): Boolean {
        return type.pythonTypeName() == pythonIteratorClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: FuzzedUtType) = sequence {
        val param = type.utType.pythonAnnotationParameters()
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
                modify = Routine.ForEach(param.toFuzzed().activateAnyIf(type)) { self, i, values ->
                    (self.tree as PythonTree.IteratorNode).items[i] = values.first().tree
                }
            ))
    }
}