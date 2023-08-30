package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.IEEE754Value
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonFloatClassId
import org.utbot.python.framework.api.python.util.pythonIntClassId
import org.utbot.python.fuzzing.PythonFuzzedConcreteValue
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.generateSummary
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.pythonTypeName
import java.math.BigDecimal
import java.math.BigInteger

object FloatValueProvider : ValueProvider<UtType, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: UtType): Boolean {
        return type.pythonTypeName() == pythonFloatClassId.canonicalName || type.isAny()
    }

    private fun getFloatConstants(concreteValues: Collection<PythonFuzzedConcreteValue>): List<IEEE754Value> {
        return concreteValues
            .filter { accept(it.type) }
            .map { fuzzedValue ->
                (fuzzedValue.value as BigDecimal).let {
                    IEEE754Value.fromValue(it.toDouble())
                }
            }
    }

    private fun getIntConstants(concreteValues: Collection<PythonFuzzedConcreteValue>): List<IEEE754Value> {
        return concreteValues
            .filter { it.type.pythonTypeName() == pythonIntClassId.canonicalName }
            .map { fuzzedValue ->
                (fuzzedValue.value as BigInteger).let {
                    IEEE754Value.fromValue(it.toDouble())
                }
            }
    }

    override fun generate(description: PythonMethodDescription, type: UtType): Sequence<Seed<UtType, PythonFuzzedValue>> = sequence {
        val floatConstants = getFloatConstants(description.concreteValues)
        val intConstants = getIntConstants(description.concreteValues)
        val constants = floatConstants + intConstants

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