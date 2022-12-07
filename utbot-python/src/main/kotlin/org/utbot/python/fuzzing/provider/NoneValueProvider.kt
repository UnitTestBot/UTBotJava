package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonNoneClassId
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.PythonNoneTypeDescription
import org.utbot.python.newtyping.general.Type

object NoneValueProvider : ValueProvider<Type, PythonTreeModel, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        return type.isAny() || type.meta is PythonNoneTypeDescription
    }

    override fun generate(description: PythonMethodDescription, type: Type): Sequence<Seed<Type, PythonTreeModel>> = sequence {
        yield(Seed.Simple(PythonTreeModel(PythonTree.fromNone(), pythonNoneClassId)))
    }
}