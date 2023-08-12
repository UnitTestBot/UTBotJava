package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonBytesClassId
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.newtyping.pythonTypeRepresentation

object BytesValueProvider : ValueProvider<UtType, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: UtType): Boolean {
        return type.pythonTypeName() == pythonBytesClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: UtType) = sequence {
        yield(Seed.Recursive(
            construct = Routine.Create(
                listOf(
                    description.pythonTypeStorage.pythonInt,
                )
            ) { v ->
                val value = v.first().tree as PythonTree.PrimitiveNode
                PythonFuzzedValue(
                    PythonTree.PrimitiveNode(
                        pythonBytesClassId,
                        "bytes(${value.repr})"
                    ),
                    "%var% = ${type.pythonTypeRepresentation()}",
                )
            },
            empty = Routine.Empty {
                PythonFuzzedValue(
                    PythonTree.PrimitiveNode(
                        pythonBytesClassId,
                        "bytes()"
                    ),
                    "%var% = ${type.pythonTypeRepresentation()}"
                )
            }
        ))
    }
}