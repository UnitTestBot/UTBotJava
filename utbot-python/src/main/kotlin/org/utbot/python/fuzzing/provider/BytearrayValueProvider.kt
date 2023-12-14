package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonBytearrayClassId
import org.utbot.python.fuzzing.FuzzedUtType
import org.utbot.python.fuzzing.FuzzedUtType.Companion.toFuzzed
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.PythonValueProvider

object BytearrayValueProvider : PythonValueProvider {
    override fun accept(type: FuzzedUtType): Boolean {
        return type.pythonTypeName() == pythonBytearrayClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: FuzzedUtType) = sequence {
        yield(Seed.Recursive(
            construct = Routine.Create(
                listOf(
                    description.pythonTypeStorage.pythonInt,
                ).toFuzzed()
            ) { v ->
                val value = v.first().tree as PythonTree.PrimitiveNode
                PythonFuzzedValue(
                    PythonTree.PrimitiveNode(
                        pythonBytearrayClassId,
                        "bytearray(${value.repr})"
                    ),
                    "%var% = ${type.pythonTypeRepresentation()}",
                )
            },
            empty = Routine.Empty { PythonFuzzedValue(
                PythonTree.PrimitiveNode(
                    pythonBytearrayClassId,
                    "bytearray()"
                ),
                "%var% = ${type.pythonTypeRepresentation()}"
            ) }
        ))
    }
}