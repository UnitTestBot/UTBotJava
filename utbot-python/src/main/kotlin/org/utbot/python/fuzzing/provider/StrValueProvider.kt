package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.fuzzing.seeds.StringValue
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonStrClassId
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.general.Type

object StrValueProvider : ValueProvider<Type, PythonTreeModel, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        val meta = type.meta
        return (meta is PythonConcreteCompositeTypeDescription) && meta.name.toString() == "builtins.str"
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        yieldStrings(StringValue("test")) { value }
        yieldStrings(StringValue("abc")) { value }
        yieldStrings(StringValue("")) { value }
    }

    private suspend fun <T : KnownValue> SequenceScope<Seed<Type, PythonTreeModel>>.yieldStrings(value: T, block: T.() -> Any) {
        yield(Seed.Known(value) { PythonTreeModel(PythonTree.fromString(block(it).toString()), pythonStrClassId) })
    }
}