package fuzzer.providers

import org.utbot.framework.plugin.api.js.JsPrimitiveModel
import org.utbot.framework.plugin.api.js.util.jsStringClassId
import org.utbot.framework.plugin.api.js.util.toJsClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedOp
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.types.WithClassId
import kotlin.random.Random

object JsStringModelProvider : ModelProvider {

    internal val random = Random(72923L)

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.concreteValues
            .asSequence()
            .filter { (classId, _) -> (classId as WithClassId).classId.toJsClassId() == jsStringClassId }
            .forEach { (_, value, op) ->
                listOf(value, mutate(random, value as? String, op as FuzzedOp))
                    .asSequence()
                    .filterNotNull()
                    .map { JsPrimitiveModel(it) }.forEach { model ->
                        description.parametersMap.keys.indices.forEach { index ->
                            yield(FuzzedParameter(index, model.fuzzed { summary = "%var% = string" }))
                        }
                    }
            }
    }

    fun mutate(random: Random, value: String?, op: FuzzedOp): String? {
        if (value.isNullOrEmpty() || op != FuzzedOp.CH) return null
        val indexOfMutation = random.nextInt(value.length)
        return value.replaceRange(
            indexOfMutation,
            indexOfMutation + 1,
            SingleCharacterSequence(value[indexOfMutation] - random.nextInt(1, 128))
        )
    }

    private class SingleCharacterSequence(private val character: Char) : CharSequence {
        override val length: Int
            get() = 1

        override fun get(index: Int): Char = character

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            throw UnsupportedOperationException()
        }

    }
}