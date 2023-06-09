@file:Suppress("ReplaceRangeStartEndInclusiveWithFirstLast")

package org.utbot.fuzzing

import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.IEEE754Value
import org.utbot.fuzzing.seeds.StringValue
import kotlin.random.Random

/**
 * Mutations is an object which applies some changes to the source object
 * and then returns a new object (or old one without changes).
 */
fun interface Mutation<T> {
    fun mutate(source: T, random: Random, configuration: Configuration): T
}

inline fun <reified T, reified F : T> Mutation<F>.adapt(): Mutation<T> {
    return Mutation { s, r, c ->
        if (s is F) return@Mutation mutate(s, r, c) else s
    }
}

sealed class BitVectorMutations : Mutation<BitVectorValue> {

    abstract fun rangeOfMutation(source: BitVectorValue): IntRange

    override fun mutate(source: BitVectorValue, random: Random, configuration: Configuration): BitVectorValue {
        with (rangeOfMutation(source)) {
            val firstBits = random.nextInt(start, endInclusive.coerceAtLeast(1))
            return BitVectorValue(source, this@BitVectorMutations).apply { this[firstBits] = !this[firstBits] }
        }
    }

    object SlightDifferent : BitVectorMutations() {
        override fun rangeOfMutation(source: BitVectorValue) = 0 ..  source.size / 4
    }

    object DifferentWithSameSign : BitVectorMutations() {
        override fun rangeOfMutation(source: BitVectorValue) = source.size / 4 .. source.size
    }

    object ChangeSign : BitVectorMutations() {
        override fun rangeOfMutation(source: BitVectorValue) = source.size - 1 .. source.size
    }
}

sealed class IEEE754Mutations : Mutation<IEEE754Value> {

    object ChangeSign : IEEE754Mutations() {
        override fun mutate(source: IEEE754Value, random: Random, configuration: Configuration): IEEE754Value {
            return IEEE754Value(source, this).apply {
                setRaw(0, !getRaw(0))
            }
        }
    }

    object Mantissa : IEEE754Mutations() {
        override fun mutate(source: IEEE754Value, random: Random, configuration: Configuration): IEEE754Value {
            val i = random.nextInt(0, source.mantissaSize)
            return IEEE754Value(source, this).apply {
                setRaw(1 + exponentSize + i, !getRaw(1 + exponentSize + i))
            }
        }
    }

    object Exponent : IEEE754Mutations() {
        override fun mutate(source: IEEE754Value, random: Random, configuration: Configuration): IEEE754Value {
            val i = random.nextInt(0, source.exponentSize)
            return IEEE754Value(source, this).apply {
                setRaw(1 + i, !getRaw(1 + i))
            }
        }
    }
}

sealed class StringMutations : Mutation<StringValue> {

    object AddCharacter : StringMutations() {
        override fun mutate(source: StringValue, random: Random, configuration: Configuration): StringValue {
            val value = source.value
            if (value.length >= configuration.maxStringLengthWhenMutated) {
                return source
            }
            val position = random.nextInt(value.length + 1)
            val charToMutate = if (value.isNotEmpty()) {
                value.random(random)
            } else {
                // use any meaningful character from the ascii table
                random.nextInt(33, 127).toChar()
            }
            val newString = buildString {
                append(value.substring(0, position))
                // try to change char to some that is close enough to origin char
                val charTableSpread = 64
                if (random.nextBoolean()) {
                    append(charToMutate - random.nextInt(1, charTableSpread))
                } else {
                    append(charToMutate + random.nextInt(1, charTableSpread))
                }
                append(value.substring(position, value.length))
            }
            return StringValue(newString, lastMutation = this)
        }
    }

    object RemoveCharacter : StringMutations() {
        override fun mutate(source: StringValue, random: Random, configuration: Configuration): StringValue {
            val value = source.value
            val position = random.nextInt(value.length + 1)
            if (position >= value.length) return source
            val toRemove = random.nextInt(value.length)
            val newString = buildString {
                append(value.substring(0, toRemove))
                append(value.substring(toRemove + 1, value.length))
            }
            return StringValue(newString, this)
        }
    }

}