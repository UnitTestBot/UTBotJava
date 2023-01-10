package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.Signed
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonIntClassId
import org.utbot.python.fuzzing.PythonFuzzedValue
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.generateSummary
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonTypeName
import java.math.BigInteger

object IntValueProvider : ValueProvider<Type, PythonFuzzedValue, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        return type.pythonTypeName() == "builtins.int" || type.isAny()
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence<Seed<Type, PythonFuzzedValue>> {
        val bits = 128
        val integerConstants = listOf(
            BitVectorValue.fromBigInteger(BigInteger("239")),
            BitVectorValue.fromBigInteger(BigInteger("100")),
            BitVectorValue.fromBigInteger(BigInteger("-100")),
        )

        val modifiedConstants = sequence {
            integerConstants.forEach {
                val valueInc = it
                val valueDec = it
                valueInc.inc()
                valueDec.dec()
                yield(valueDec)
                yield(valueInc)
            }
        }

        val constants = integerConstants + modifiedConstants + Signed.values().map { it.invoke(bits) }

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