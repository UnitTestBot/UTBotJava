package org.utbot.language.ts.fuzzer.providers

import org.utbot.language.ts.framework.api.ts.TsClassId
import org.utbot.language.ts.framework.api.ts.TsPrimitiveModel
import org.utbot.language.ts.framework.api.ts.util.isTsPrimitive
import org.utbot.language.ts.framework.api.ts.util.tsUndefinedClassId
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider

object TsConstantsModelProvider : ModelProvider {

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.concreteValues
            .asSequence()
            .filter { (classId, _) ->
                (classId as TsClassId).isTsPrimitive
            }
            .forEach { (_, value, op) ->
                sequenceOf(
                    TsPrimitiveModel(value).fuzzed { summary = "%var% = $value" },
                    modifyValue(value, op)
                )
                    .filterNotNull()
                    .forEach { m ->
                        description.parametersMap.getOrElse(m.model.classId) {
                            description.parametersMap.getOrElse(tsUndefinedClassId) { emptyList() }
                        }.forEach { index ->
                            yield(FuzzedParameter(index, m))
                        }
                    }
            }
    }

    @Suppress("DuplicatedCode")
    private fun modifyValue(value: Any, op: FuzzedContext): FuzzedValue? {
        if (op !is FuzzedContext.Comparison) return null
        val multiplier = if (op == FuzzedContext.Comparison.LT || op == FuzzedContext.Comparison.GE) -1 else 1
        return when (value) {
            is Boolean -> value.not()
            is Byte -> value + multiplier.toByte()
            is Char -> (value.code + multiplier).toChar()
            is Short -> value + multiplier.toShort()
            is Int -> value + multiplier
            is Long -> value + multiplier.toLong()
            is Float -> value + multiplier.toDouble()
            is Double -> value + multiplier.toDouble()
            else -> null
        }?.let {
            TsPrimitiveModel(it).fuzzed {
                summary = "%var% ${
                    (if (op == FuzzedContext.Comparison.EQ || op == FuzzedContext.Comparison.LE || op == FuzzedContext.Comparison.GE) {
                        op.reverse()
                    } else op).sign
                } $value"
            }
        }
    }
}