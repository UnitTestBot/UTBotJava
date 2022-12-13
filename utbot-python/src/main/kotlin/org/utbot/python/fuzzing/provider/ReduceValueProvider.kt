package org.utbot.python.fuzzing.provider

import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.general.FunctionTypeCreator
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.getPythonAttributes

class ReduceValueProvider(
    private val idGenerator: IdGenerator<Long>
) : ValueProvider<Type, PythonTreeModel, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        val hasInit = type.getPythonAttributes().any { it.name == "__init__" && it.type is FunctionTypeCreator.Original }
        return type.meta is PythonConcreteCompositeTypeDescription && hasInit
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val meta = type.meta as PythonConcreteCompositeTypeDescription
        val arguments = (type.getPythonAttributes().first { it.name == "__init__" }.type as FunctionTypeCreator.Original).arguments
        val nonSelfArgs = arguments.drop(1)
        yield(Seed.Recursive(
            construct = Routine.Create(nonSelfArgs) { v ->
                PythonTreeModel(
                    PythonTree.ReduceNode(
                        idGenerator.createId(),
                        PythonClassId(meta.name.toString()),
                        PythonClassId(meta.name.toString()),
                        v.map { it.tree },
                    ),
                    PythonClassId(meta.name.toString())
                )
            },
            empty = Routine.Empty { PythonTreeModel(PythonTree.fromNone(), PythonClassId(meta.name.toString())) }
        ))
    }
}