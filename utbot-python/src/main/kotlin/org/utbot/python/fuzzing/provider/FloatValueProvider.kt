package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.DefaultFloatBound
import org.utbot.fuzzing.seeds.IEEE754Value
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.generateSummary
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonTypeName

object FloatValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        return type.pythonTypeName() == "builtins.float" || type.isAny()
    }

    override fun generate(description: PythonMethodDescription, type: Type): Sequence<Seed<Type, PythonFuzzedValue>> = sequence {
        val floatConstants = listOf(
            IEEE754Value.fromDouble(23.9),
            IEEE754Value.fromDouble(1.6),
            IEEE754Value.fromDouble(-1.6),
        )

        val constants = floatConstants + DefaultFloatBound.values().map {
            it(52, 11)
        }

        constants.asSequence().forEach {  value ->
            yield(Seed.Known(value) {
                PythonFuzzedValue(
                    PythonTree.fromFloat(it.toDouble()),
                    it.generateSummary()
                )
            })
        }
    }
}