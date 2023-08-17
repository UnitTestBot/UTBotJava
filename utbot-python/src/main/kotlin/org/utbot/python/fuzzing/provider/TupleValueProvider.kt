package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonTupleClassId
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.getSuitableConstantsFromCode
import org.utbot.python.newtyping.*
import org.utbot.python.newtyping.general.UtType

object TupleValueProvider : ValueProvider<UtType, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: UtType): Boolean {
        return type.pythonTypeName() == pythonTupleClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: UtType) = sequence {
        yieldAll(getConstants(description, type))
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

    private fun getConstants(description: PythonMethodDescription, type: UtType): List<Seed<UtType, PythonFuzzedValue>> {
        if (!typesAreEqual(type.parameters.first(), pythonAnyType))
            return getSuitableConstantsFromCode(description, type)
        return emptyList()
    }
}