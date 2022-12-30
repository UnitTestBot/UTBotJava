package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonBytesClassId
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.general.Type

object BytesValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        val meta = type.meta
        return (meta is PythonConcreteCompositeTypeDescription) && meta.name.toString() == "builtins.bytearray"
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val meta = type.meta as PythonConcreteCompositeTypeDescription
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
                )
            },
            empty = Routine.Empty {
                PythonFuzzedValue(
                    PythonTree.PrimitiveNode(
                        pythonBytesClassId,
                        "bytes()"
                    ),
                    "%var% = ${meta.name}"
                )
            }
        ))
    }
}