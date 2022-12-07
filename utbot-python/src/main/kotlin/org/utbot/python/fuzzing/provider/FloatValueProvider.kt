package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.fuzzing.seeds.Signed
import org.utbot.fuzzing.seeds.StringValue
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonFloatClassId
import org.utbot.python.framework.api.python.util.pythonIntClassId
import org.utbot.python.framework.api.python.util.pythonStrClassId
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.general.Type

object FloatValueProvider : ValueProvider<Type, PythonTreeModel, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        val meta = type.meta
        if (meta is PythonConcreteCompositeTypeDescription) {
            return meta.name.toString() == "builtins.float"
        }
        return type.isAny()
    }

    override fun generate(description: PythonMethodDescription, type: Type): Sequence<Seed<Type, PythonTreeModel>> = sequence {
        yield(Seed.Simple(PythonTreeModel(PythonTree.fromFloat(1.1), pythonFloatClassId)))
        yield(Seed.Simple(PythonTreeModel(PythonTree.fromFloat(1.0), pythonFloatClassId)))
        yield(Seed.Simple(PythonTreeModel(PythonTree.fromFloat(1.6), pythonFloatClassId)))
        yield(Seed.Simple(PythonTreeModel(PythonTree.fromFloat(-1.6), pythonFloatClassId)))
        yield(Seed.Simple(PythonTreeModel(PythonTree.fromFloat(-1.0), pythonFloatClassId)))
        yield(Seed.Simple(PythonTreeModel(PythonTree.fromFloat(0.0), pythonFloatClassId)))
    }
}