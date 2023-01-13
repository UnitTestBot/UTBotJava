package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.Bool
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.generateSummary
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonTypeName

object BoolValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription>{
    override fun accept(type: Type): Boolean {
        return type.pythonTypeName() == "builtins.bool" || type.isAny()
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        yieldBool(Bool.TRUE()) { true }
        yieldBool(Bool.FALSE()) { false }
    }

    private suspend fun <T : KnownValue> SequenceScope<Seed<Type, PythonFuzzedValue>>.yieldBool(value: T, block: T.() -> Boolean) {
        yield(Seed.Known(value) {
            PythonFuzzedValue(
                PythonTree.fromBool(block(it)),
                it.generateSummary(),
            )
        })
    }
}