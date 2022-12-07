package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.Bool
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonBoolClassId
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.general.Type

object BoolValueProvider : ValueProvider<Type, PythonTreeModel, PythonMethodDescription>{
    override fun accept(type: Type): Boolean {
        val meta = type.meta
        if (meta is PythonConcreteCompositeTypeDescription) {
            return meta.name.toString() == "builtins.bool"
        }
        return type.isAny()
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        yieldBool(Bool.TRUE()) { true }
        yieldBool(Bool.FALSE()) { false }
    }

    private suspend fun <T : KnownValue> SequenceScope<Seed<Type, PythonTreeModel>>.yieldBool(value: T, block: T.() -> Boolean) {
        yield(Seed.Known(value) { PythonTreeModel(PythonTree.fromBool(block(it)), pythonBoolClassId) })
    }
}