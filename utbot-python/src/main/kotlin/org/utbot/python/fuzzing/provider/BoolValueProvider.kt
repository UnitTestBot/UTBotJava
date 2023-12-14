package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.seeds.Bool
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonBoolClassId
import org.utbot.python.fuzzing.FuzzedUtType
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.PythonValueProvider
import org.utbot.python.fuzzing.provider.utils.generateSummary

object BoolValueProvider : PythonValueProvider {
    override fun accept(type: FuzzedUtType): Boolean {
        return type.pythonTypeName() == pythonBoolClassId.canonicalName
    }

    override fun generate(description: PythonMethodDescription, type: FuzzedUtType) = sequence {
        yieldBool(Bool.TRUE()) { true }
        yieldBool(Bool.FALSE()) { false }
    }

    private suspend fun <T : KnownValue<T>> SequenceScope<Seed<FuzzedUtType, PythonFuzzedValue>>.yieldBool(value: T, block: T.() -> Boolean) {
        yield(Seed.Known(value) {
            PythonFuzzedValue(
                PythonTree.fromBool(block(it)),
                it.generateSummary(),
            )
        })
    }
}