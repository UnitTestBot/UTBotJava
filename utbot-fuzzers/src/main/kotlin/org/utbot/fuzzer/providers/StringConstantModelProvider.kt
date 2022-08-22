package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedOp
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue
import kotlin.random.Random

object StringConstantModelProvider : ModelProvider {

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        val random = Random(72923L)
        description.concreteValues
            .asSequence()
            .filter { (classId, _) -> classId == stringClassId }
            .forEach { (_, value, op) ->
                listOf(value, mutate(random, value as? String, op))
                    .asSequence()
                    .filterNotNull()
                    .map { UtPrimitiveModel(it) }.forEach { model ->
                        description.parametersMap.getOrElse(model.classId) { emptyList() }.forEach { index ->
                            yieldValue(index, model.fuzzed { summary = "%var% = string" })
                        }
                    }
            }
    }

    private fun mutate(random: Random, value: String?, op: FuzzedOp): String? {
        if (value == null || value.isEmpty() || op != FuzzedOp.CH) return null
        val indexOfMutation = random.nextInt(value.length)
        return value.replaceRange(indexOfMutation, indexOfMutation + 1, SingleCharacterSequence(value[indexOfMutation] - random.nextInt(1, 128)))
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