package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.Bool
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonBoolClassId
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.generateSummary
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.pythonTypeName

object BoolValueProvider : ValueProvider<UtType, PythonFuzzedValue, PythonMethodDescription>{
    override fun accept(type: UtType): Boolean {
        return type.pythonTypeName() == pythonBoolClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: UtType) = sequence {
        yieldBool(Bool.TRUE()) { true }
        yieldBool(Bool.FALSE()) { false }
    }

    private suspend fun <T : KnownValue<T>> SequenceScope<Seed<UtType, PythonFuzzedValue>>.yieldBool(value: T, block: T.() -> Boolean) {
        yield(Seed.Known(value) {
            PythonFuzzedValue(
                PythonTree.fromBool(block(it)),
                it.generateSummary(),
            )
        })
    }
}