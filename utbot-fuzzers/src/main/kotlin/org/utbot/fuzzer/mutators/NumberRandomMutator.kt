package org.utbot.fuzzer.mutators

import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelMutator
import org.utbot.fuzzer.invertBit
import kotlin.random.Random

/**
 * Mutates any [Number] changing random bit.
 */
object NumberRandomMutator : ModelMutator {

    override fun mutate(
        description: FuzzedMethodDescription,
        index: Int,
        value: FuzzedValue,
        random: Random
    ): FuzzedValue? {
        val model = value.model
        return if (model is UtPrimitiveModel && model.value is Number) {
            val newValue = changeRandomBit(random, model.value as Number)
            UtPrimitiveModel(newValue).mutatedFrom(value) {
                summary = "%var% = $newValue (mutated from ${model.value})"
            }
        } else null
    }

    private fun changeRandomBit(random: Random, number: Number): Number {
        val size = when (number) {
            is Byte -> Byte.SIZE_BITS
            is Short -> Short.SIZE_BITS
            is Int -> Int.SIZE_BITS
            is Float -> Float.SIZE_BITS
            is Long -> Long.SIZE_BITS
            is Double -> Double.SIZE_BITS
            else -> error("Unknown type: ${number.javaClass}")
        }
        val asLong = when (number) {
            is Byte, is Short, is Int -> number.toLong()
            is Long -> number
            is Float -> number.toRawBits().toLong()
            is Double -> number.toRawBits()
            else -> error("Unknown type: ${number.javaClass}")
        }
        val bitIndex = random.nextInt(size)
        val mutated = asLong.invertBit(bitIndex)
        return when (number) {
            is Byte -> mutated.toByte()
            is Short -> mutated.toShort()
            is Int -> mutated.toInt()
            is Float -> Float.fromBits(mutated.toInt())
            is Long -> mutated
            is Double -> Double.fromBits(mutated)
            else -> error("Unknown type: ${number.javaClass}")
        }
    }
}

