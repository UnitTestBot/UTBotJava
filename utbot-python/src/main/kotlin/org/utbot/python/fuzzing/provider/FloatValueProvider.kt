package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.seeds.IEEE754Value
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonFloatClassId
import org.utbot.python.framework.api.python.util.pythonIntClassId
import org.utbot.python.fuzzing.FuzzedUtType
import org.utbot.python.fuzzing.FuzzedUtType.Companion.toFuzzed
import org.utbot.python.fuzzing.PythonFuzzedConcreteValue
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.PythonValueProvider
import org.utbot.python.fuzzing.provider.utils.generateSummary
import org.utpython.types.pythonTypeName
import java.math.BigDecimal
import java.math.BigInteger

object FloatValueProvider : PythonValueProvider {
    override fun accept(type: FuzzedUtType): Boolean {
        return type.pythonTypeName() == pythonFloatClassId.canonicalName
    }

    private fun getFloatConstants(concreteValues: Collection<PythonFuzzedConcreteValue>): List<IEEE754Value> {
        return concreteValues
            .filter { accept(it.type.toFuzzed()) }
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

    override fun generate(description: PythonMethodDescription, type: FuzzedUtType): Sequence<Seed<FuzzedUtType, PythonFuzzedValue>> = sequence {
        val floatConstants = getFloatConstants(description.concreteValues)
        val intConstants = getIntConstants(description.concreteValues)
        val constants = floatConstants + intConstants + listOf(0, 1).map { IEEE754Value.fromValue(it.toDouble()) }

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