package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.fuzzing.seeds.Signed
import org.utbot.fuzzing.seeds.StringValue
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonIntClassId
import org.utbot.python.framework.api.python.util.pythonListClassId
import org.utbot.python.framework.api.python.util.pythonStrClassId
import org.utbot.python.framework.api.python.util.pythonTupleClassId
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.PythonUnionTypeDescription
import org.utbot.python.newtyping.general.Type

object UnionValueProvider : ValueProvider<Type, PythonTreeModel, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        return type.meta is PythonUnionTypeDescription
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val meta = type.meta as PythonUnionTypeDescription
        val params = meta.getAnnotationParameters(type)
        params.forEach { unionParam ->
            yield(Seed.Recursive(
                construct = Routine.Create(listOf(unionParam)) { v -> v.first() },
                empty = Routine.Empty { PythonTreeModel(PythonTree.fromNone(), PythonClassId(meta.name.toString())) }
            ))
        }
    }
}