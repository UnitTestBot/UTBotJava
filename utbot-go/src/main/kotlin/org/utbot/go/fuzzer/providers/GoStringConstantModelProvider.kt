package org.utbot.go.fuzzer.providers

import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedOp
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue
import org.utbot.go.api.GoUtPrimitiveModel
import org.utbot.go.api.util.goStringTypeId
import kotlin.random.Random

// This class is a copy of StringConstantModelProvider up to goStringClassId and GoUtPrimitiveModel usage.
@Suppress("DuplicatedCode")
object GoStringConstantModelProvider : ModelProvider {

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        val random = Random(72923L)
        description.concreteValues
            .asSequence()
            .filter { (classId, _) -> classId == goStringTypeId }
            .forEach { (_, value, op) ->
                listOf(value, mutate(random, value as? String, op as FuzzedOp))
                    .asSequence()
                    .filterNotNull()
                    .map { GoUtPrimitiveModel(it, goStringTypeId) }.forEach { model ->
                        description.parametersMap.getOrElse(model.classId) { emptyList() }.forEach { index ->
                            yieldValue(index, model.fuzzed { summary = "%var% = string" })
                        }
                    }
            }
    }

    private fun mutate(random: Random, value: String?, op: FuzzedOp): String? {
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