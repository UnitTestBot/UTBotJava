package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonComplexClassId
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.general.Type

object ComplexValueProvider : ValueProvider<Type, PythonTreeModel, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        val meta = type.meta
        if (meta is PythonConcreteCompositeTypeDescription) {
            return meta.name.toString() == "builtins.complex"
        }
        return type.isAny()
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val meta = type.meta as PythonConcreteCompositeTypeDescription
        yield(Seed.Recursive(
            construct = Routine.Create(
                listOf(
                    description.pythonTypeStorage.pythonFloat,
                    description.pythonTypeStorage.pythonFloat,
                )
            ) { v ->
                val real = v[0].tree as PythonTree.PrimitiveNode
                val imag = v[1].tree as PythonTree.PrimitiveNode
                PythonTreeModel(
                    PythonTree.PrimitiveNode(
                        pythonComplexClassId,
                        "complex(real=${real.repr}, imag=${imag.repr})"
                    ),
                    pythonComplexClassId,
                )
            },
            empty = Routine.Empty { PythonTreeModel(PythonTree.fromObject(), PythonClassId(meta.name.toString())) }
        ))
    }
}