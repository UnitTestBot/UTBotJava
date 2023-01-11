package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.DefaultFloatBound
import org.utbot.fuzzing.seeds.IEEE754Value
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.fuzzing.PythonFuzzedConcreteValue
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.generateSummary
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonTypeName
import java.math.BigDecimal

object FloatValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        return type.pythonTypeName() == "builtins.float" || type.isAny()
    }

    private fun getFloatConstants(concreteValues: Collection<PythonFuzzedConcreteValue>): List<IEEE754Value> {
        return concreteValues
            .filter { accept(it.classId) }
            .map { fuzzedValue ->
                (fuzzedValue.value as BigDecimal).let {
                    IEEE754Value.fromValue(it.toDouble())
                }
            }
    }

    override fun generate(description: PythonMethodDescription, type: Type): Sequence<Seed<Type, PythonFuzzedValue>> = sequence {
        val floatConstants = getFloatConstants(description.concreteValues)
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