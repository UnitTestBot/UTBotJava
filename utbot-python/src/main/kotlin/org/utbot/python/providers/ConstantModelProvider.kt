package org.utbot.python.providers

import org.utbot.framework.plugin.api.python.PythonClassId
import org.utbot.framework.plugin.api.python.PythonPrimitiveModel
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue
import org.utbot.fuzzer.types.ClassIdWrapper
import java.math.BigDecimal
import java.math.BigInteger

class ConstantModelProvider(recursionDepth: Int): PythonModelProvider(recursionDepth) {

    override fun generate(description: PythonFuzzedMethodDescription) = sequence {
        description.concreteValues
            .asSequence()
            .flatMap { (classId, value, op) ->
                val model = ((classId as ClassIdWrapper).classId as? PythonClassId)?.let { PythonPrimitiveModel(value, it) }
                sequenceOf(
                    model?.fuzzed { summary = "%var% = $value" },
                    modifyValue(model, op)
                )
            }
            .filterNotNull()
            .forEach { value ->
                description.parametersMap.getOrElse(ClassIdWrapper(value.model.classId)) { emptyList() }.forEach { index ->
                    yieldValue(index, value)
                }
            }
    }

    private fun modifyValue(model: PythonPrimitiveModel?, op: FuzzedContext): FuzzedValue? {
        if (op !is FuzzedContext.Comparison || model == null) return null
        val multiplier = if (op == FuzzedContext.Comparison.LT || op == FuzzedContext.Comparison.GE) -1 else 1
        return when (val value = model.value) {
            is BigInteger -> value + multiplier.toBigInteger()
            is BigDecimal -> value + multiplier.toBigDecimal()
            else -> null
        }?.let { PythonPrimitiveModel(it, model.classId as PythonClassId).fuzzed { summary = "%var% ${
            (if (op == FuzzedContext.Comparison.EQ || op == FuzzedContext.Comparison.LE || op == FuzzedContext.Comparison.GE) {
                op.reverse()
            } else op).sign
        } ${model.value}" } }
    }
}