package fuzzer.providers

import framework.api.js.JsClassId
import framework.api.js.JsPrimitiveModel
import framework.api.js.util.isJsPrimitive
import framework.api.js.util.jsUndefinedClassId
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider

object JsConstantsModelProvider : ModelProvider {

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.concreteValues
            .asSequence()
            .filter { (classId, _) ->
                (classId as JsClassId).isJsPrimitive
            }
            .forEach { (_, value, op) ->
                sequenceOf(
                    JsPrimitiveModel(value).fuzzed { summary = "%var% = $value" },
                    modifyValue(value, op as FuzzedContext.Comparison)
                )
                    .filterNotNull()
                    .forEach { m ->
                        description.parametersMap.getOrElse(m.model.classId) {
                            description.parametersMap.getOrElse(jsUndefinedClassId) { emptyList() }
                        }.forEach { index ->
                            yield(FuzzedParameter(index, m))
                        }
                    }
            }
    }

    @Suppress("DuplicatedCode")
    internal fun modifyValue(value: Any, op: FuzzedContext.Comparison): FuzzedValue? {
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
            JsPrimitiveModel(it).fuzzed {
                summary = "%var% ${
                    (if (op == FuzzedContext.Comparison.EQ || op == FuzzedContext.Comparison.LE || op == FuzzedContext.Comparison.GE) {
                        op.reverse()
                    } else op).sign
                } $value"
            }
        }
    }
}