package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonComplexClassId
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.createPythonUnionType
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.newtyping.pythonTypeRepresentation

object ComplexValueProvider : ValueProvider<UtType, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: UtType): Boolean {
        return type.pythonTypeName() == pythonComplexClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: UtType) = sequence {
        val numberType = createPythonUnionType(
            listOf(
                description.pythonTypeStorage.pythonFloat,
                description.pythonTypeStorage.pythonInt
            )
        )
        val emptyValue =
            PythonFuzzedValue(
                PythonTree.PrimitiveNode(
                    pythonComplexClassId,
                    "complex()"
                ),
                "%var% = ${type.pythonTypeRepresentation()}"
            )
        yield(Seed.Recursive(
            construct = Routine.Create(
                listOf(
                    numberType,
                    numberType
                )
            ) { v ->
                if (v[0].tree is PythonTree.FakeNode || v[1].tree is PythonTree.FakeNode) {
                    emptyValue
                } else {
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
                }
            },
            empty = Routine.Empty { emptyValue }
        ))
    }
}