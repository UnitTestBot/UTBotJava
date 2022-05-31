package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedOp
import org.utbot.fuzzer.ModelProvider
import java.util.function.BiConsumer

/**
 * Traverses through method constants and creates appropriate models for them.
 */
object ConstantsModelProvider : ModelProvider {

    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
        description.concreteValues
            .asSequence()
            .filter { (classId, _) -> classId.isPrimitive }
            .forEach { (_, value, op) ->
                sequenceOf(value, modifyValue(value, op))
                    .filterNotNull()
                    .map(::UtPrimitiveModel)
                    .forEach { model ->
                        description.parametersMap.getOrElse(model.classId) { emptyList() }.forEach { index ->
                            consumer.accept(index, model)
                        }
                    }
        }
    }

    private fun modifyValue(value: Any, op: FuzzedOp): Any? {
        if (!op.isComparisonOp()) return null
        val multiplier = if (op == FuzzedOp.LT || op == FuzzedOp.GE) -1 else 1
        return when(value) {
            is Boolean -> value.not()
            is Byte -> value + multiplier.toByte()
            is Char -> (value.toInt() + multiplier).toChar()
            is Short -> value + multiplier.toShort()
            is Int -> value + multiplier
            is Long -> value + multiplier.toLong()
            is Float -> value + multiplier.toDouble()
            is Double -> value + multiplier.toDouble()
            else -> null
        }
    }
}