package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonComplexClassId
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.newtyping.pythonTypeRepresentation

object ComplexValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        return type.pythonTypeName() == pythonComplexClassId.canonicalName || type.isAny()
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        yield(Seed.Recursive(
            construct = Routine.Create(
                listOf(
                    description.pythonTypeStorage.pythonFloat,
                    description.pythonTypeStorage.pythonFloat,
                )
            ) { v ->
                val real = v[0].tree as PythonTree.PrimitiveNode
                val imag = v[1].tree as PythonTree.PrimitiveNode
                val repr = "complex(real=${real.repr}, imag=${imag.repr})"
                PythonFuzzedValue(
                    PythonTree.PrimitiveNode(
                        pythonComplexClassId,
                        repr
                    ),
                    "%var% = $repr"
                )
            },
            empty = Routine.Empty {
                PythonFuzzedValue(
                    PythonTree.PrimitiveNode(
                        pythonComplexClassId,
                        "complex()"
                    ),
                    "%var% = ${type.pythonTypeRepresentation()}"
                )
            }
        ))
    }
}