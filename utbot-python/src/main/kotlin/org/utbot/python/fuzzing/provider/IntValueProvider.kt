package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Configuration
import org.utbot.fuzzing.Mutation
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.KnownValue
import org.utbot.fuzzing.seeds.Signed
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonIntClassId
import org.utbot.python.fuzzing.PythonFuzzedConcreteValue
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.generateSummary
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.general.UtType
import org.utbot.python.newtyping.pythonTypeName
import java.math.BigInteger
import kotlin.random.Random

object IntValueProvider : ValueProvider<UtType, PythonFuzzedValue, PythonMethodDescription> {
    private val randomStubWithNoUsage = Random(0)
    private val configurationStubWithNoUsage = Configuration()

    override fun accept(type: UtType): Boolean {
        return type.pythonTypeName() == pythonIntClassId.canonicalName || type.isAny()
    }

    private fun BitVectorValue.change(func: BitVectorValue.() -> Unit): BitVectorValue {
        return Mutation<KnownValue<*>> { _, _, _ ->
            BitVectorValue(this).apply { func() }
        }.mutate(this, randomStubWithNoUsage, configurationStubWithNoUsage) as BitVectorValue
    }

    private fun getIntConstants(concreteValues: Collection<PythonFuzzedConcreteValue>): List<BitVectorValue> {
        return concreteValues
            .filter { accept(it.type) }
            .map { fuzzedValue ->
                (fuzzedValue.value as BigInteger).let {
                    BitVectorValue.fromBigInteger(it)
                }
            }
    }

    override fun generate(description: PythonMethodDescription, type: UtType) = sequence<Seed<UtType, PythonFuzzedValue>> {
        val bits = 128
        val integerConstants = getIntConstants(description.concreteValues)
        val modifiedConstants = integerConstants.flatMap { value ->
            listOf(
                value,
                value.change { inc() },
                value.change { dec() }
            )
        }.toSet()

        val constants = modifiedConstants + Signed.values().map { it.invoke(bits) }

        constants.asSequence().forEach { vector ->
            yield(Seed.Known(vector) {
                PythonFuzzedValue(
                    PythonTree.fromInt(it.toBigInteger()),
                    it.generateSummary()
                )
            })
        }
    }
}