package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonClassId
import org.utbot.framework.plugin.api.PythonPrimitiveModel
import org.utbot.fuzzer.FuzzedOp
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue
import java.math.BigDecimal
import java.math.BigInteger

class ConstantModelProvider(recursionDepth: Int): PythonModelProvider(recursionDepth) {

    override fun generate(description: PythonFuzzedMethodDescription) = sequence {
        description.concreteValues
            .asSequence()
            .flatMap { (classId, value, op) ->
                val model = (classId as? PythonClassId)?.let { PythonPrimitiveModel(value, it) }
                sequenceOf(
                    model?.fuzzed { summary = "%var% = $value" },
                    modifyValue(model, op)
                )
            }
            .filterNotNull()
            .forEach { value ->
                description.parametersMap.getOrElse(value.model.classId) { emptyList() }.forEach { index ->
                    yieldValue(index, value)
                }
            }
    }

    private fun modifyValue(model: PythonPrimitiveModel?, op: FuzzedOp): FuzzedValue? {
        if (!op.isComparisonOp() || model == null) return null
        val multiplier = if (op == FuzzedOp.LT || op == FuzzedOp.GE) -1 else 1
        return when (val value = model.value) {
            is BigInteger -> value + multiplier.toBigInteger()
            is BigDecimal -> value + multiplier.toBigDecimal()
            else -> null
        }?.let { PythonPrimitiveModel(it, model.classId as PythonClassId).fuzzed { summary = "%var% ${
            (if (op == FuzzedOp.EQ || op == FuzzedOp.LE || op == FuzzedOp.GE) {
                op.reverseOrNull() ?: error("cannot find reverse operation for $op")
            } else op).sign
        } ${model.value}" } }
    }
}