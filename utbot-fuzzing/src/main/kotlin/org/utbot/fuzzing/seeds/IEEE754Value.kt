package org.utbot.fuzzing.seeds

import org.utbot.fuzzing.*
import kotlin.math.pow

//val FLOAT_ZERO = DefaultFloatBound.ZERO(23, 8)
//val FLOAT_NAN = DefaultFloatBound.NAN(23, 8)
//val FLOAT_POSITIVE_INFINITY = DefaultFloatBound.POSITIVE_INFINITY(23, 8)
//val FLOAT_NEGATIVE_INFINITY = DefaultFloatBound.NEGATIVE_INFINITY(23, 8)
//val DOUBLE_ZERO = DefaultFloatBound.ZERO(52, 11)
//val DOUBLE_NAN = DefaultFloatBound.NAN(52, 11)
//val DOUBLE_POSITIVE_INFINITY = DefaultFloatBound.POSITIVE_INFINITY(52, 11)
//val DOUBLE_NEGATIVE_INFINITY = DefaultFloatBound.NEGATIVE_INFINITY(52, 11)

class IEEE754Value : KnownValue<IEEE754Value> {

    val isPositive: Boolean
        get() = !vector[0]

    val bias: Int
        get() = (2 shl (exponentSize - 2)) - 1

    val exponent: Long
        get() {
            check(exponentSize <= 64) { "Exponent cannot be represented as long" }
            var result = 0L
            for (i in 0 until  exponentSize) {
                if (vector[exponentSize - i]) {
                    result += 1L shl i
                }
            }
            return result - bias
        }

    val mantissaSize: Int
    val exponentSize: Int
    override val lastMutation: Mutation<IEEE754Value>?
    override val mutatedFrom: IEEE754Value?

    private val vector: BitVectorValue

    constructor(mantissaSize: Int, exponentSize: Int, bound: FloatBound ) {
        this.mantissaSize = mantissaSize
        this.exponentSize = exponentSize
        this.vector = BitVectorValue(1 + mantissaSize + exponentSize) { index, size ->
            check(1 + exponentSize + mantissaSize == size) { "size exceeds" }
            when {
                index >= 1 + exponentSize + mantissaSize -> error("out of range")
                index >= 1 + exponentSize -> bound.mantissa(index - 1 - exponentSize, mantissaSize)
                index >= 1 -> bound.exponent(index - 1, exponentSize)
                index == 0 -> bound.sign()
                else -> error("out of range")
            }
        }
        lastMutation = null
        mutatedFrom = null
    }

    constructor(value: IEEE754Value, mutation: Mutation<IEEE754Value>? = null) {
        this.vector = BitVectorValue(value.vector)
        this.mantissaSize = value.mantissaSize
        this.exponentSize = value.exponentSize
        this.lastMutation = mutation
        this.mutatedFrom = value
    }

    fun getRaw(index: Int) = vector[index]

    fun setRaw(index: Int, value: Boolean) {
        vector[index] = value
    }

    fun toFloat(): Float {
        DefaultFloatBound.values().forEach {
            if (it.test(this)) when (it) {
                DefaultFloatBound.ZERO -> return 0.0f
                DefaultFloatBound.NAN -> return Float.NaN
                DefaultFloatBound.POSITIVE_INFINITY -> return Float.POSITIVE_INFINITY
                DefaultFloatBound.NEGATIVE_INFINITY -> return Float.NEGATIVE_INFINITY
                else -> {}
            }
        }
        var result = 0.0f
        val e = exponent.toFloat()
        result += 2.0f.pow(e)
        for (i in 0 until mantissaSize) {
            if (vector[1 + exponentSize + i]) {
                result += 2.0f.pow(e - 1 - i)
            }
        }
        return result * if (isPositive) 1.0f else -1.0f
    }

    fun toDouble(): Double {
        DefaultFloatBound.values().forEach {
            if (it.test(this)) when (it) {
                DefaultFloatBound.ZERO -> return 0.0
                DefaultFloatBound.NAN -> return Double.NaN
                DefaultFloatBound.POSITIVE_INFINITY -> return Double.POSITIVE_INFINITY
                DefaultFloatBound.NEGATIVE_INFINITY -> return Double.NEGATIVE_INFINITY
                else -> {}
            }
        }
        var result = 0.0
        val e = exponent.toDouble()
        result += 2.0.pow(e)
        for (i in 0 until mantissaSize) {
            if (vector[1 + exponentSize + i]) {
                result += 2.0.pow(e - 1 - i)
            }
        }
        return result * if (isPositive) 1.0 else -1.0
    }

    fun is32Float(): Boolean {
        return vector.size == 32 && mantissaSize == 23 && exponentSize == 8
    }

    fun is64Float(): Boolean {
        return vector.size == 64 && mantissaSize == 52 && exponentSize == 11
    }

    override fun toString() = buildString {
        for (i in 0 until vector.size) {
            if (i == 1 || i == 1 + exponentSize) append(" ")
            append(if (getRaw(i)) '1' else '0')
        }
    }

    override fun mutations() = listOf<Mutation<IEEE754Value>>(
        IEEE754Mutations.ChangeSign,
        IEEE754Mutations.Mantissa,
        IEEE754Mutations.Exponent,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IEEE754Value

        if (mantissaSize != other.mantissaSize) return false
        if (exponentSize != other.exponentSize) return false
        if (vector != other.vector) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mantissaSize
        result = 31 * result + exponentSize
        result = 31 * result + vector.hashCode()
        return result
    }

    companion object {
        fun fromValue(value: Any): IEEE754Value {
            return when (value) {
                is Float -> fromFloat(value)
                is Double -> fromDouble(value)
                else -> error("unknown type of value $value (${value::class})")
            }
        }

        fun fromFloat(value: Float): IEEE754Value {
            return IEEE754Value(23, 8, object : FloatBound {

                val rawInt = value.toRawBits()

                override fun sign(): Boolean {
                    return rawInt and (1 shl 31) != 0
                }

                override fun mantissa(index: Int, size: Int): Boolean {
                    return rawInt and (1 shl (size - 1 - index)) != 0
                }

                override fun exponent(index: Int, size: Int): Boolean {
                    return rawInt and (1 shl (30 - index)) != 0
                }

            })
        }

        fun fromDouble(value: Double): IEEE754Value {
            return IEEE754Value(52, 11, object : FloatBound {

                val rawLong = value.toRawBits()

                override fun sign(): Boolean {
                    return rawLong and (1L shl 63) != 0L
                }

                override fun mantissa(index: Int, size: Int): Boolean {
                    return rawLong and (1L shl (size - 1 - index)) != 0L
                }

                override fun exponent(index: Int, size: Int): Boolean {
                    return rawLong and (1L shl (62 - index)) != 0L
                }

            })
        }
    }
}

interface FloatBound {
    fun sign(): Boolean
    fun mantissa(index: Int, size: Int): Boolean
    fun exponent(index: Int, size: Int): Boolean
}

private class CopyBound(val vector: IEEE754Value) : FloatBound {
    override fun sign(): Boolean = vector.getRaw(0)
    override fun exponent(index: Int, size: Int): Boolean = vector.getRaw(1 + index)
    override fun mantissa(index: Int, size: Int): Boolean = vector.getRaw(1 + vector.exponentSize + index)
}

enum class DefaultFloatBound : FloatBound {
    ZERO {
        override fun sign() = false
        override fun mantissa(index: Int, size: Int) = false
        override fun exponent(index: Int, size: Int) = false
    },
    NAN {
        override fun sign() = false
        override fun mantissa(index: Int, size: Int) = index == size - 1
        override fun exponent(index: Int, size: Int) = true
    },
    POSITIVE {
        override fun sign() = false
        override fun mantissa(index: Int, size: Int) = false
        override fun exponent(index: Int, size: Int) = index != 0
    },
    NEGATIVE {
        override fun sign() = true
        override fun mantissa(index: Int, size: Int) = false
        override fun exponent(index: Int, size: Int) = index != 0
    },
    POSITIVE_INFINITY {
        override fun sign() = false
        override fun mantissa(index: Int, size: Int) = false
        override fun exponent(index: Int, size: Int) = true
    },
    NEGATIVE_INFINITY {
        override fun sign() = true
        override fun mantissa(index: Int, size: Int) = false
        override fun exponent(index: Int, size: Int) = true
    },
    ;

    operator fun invoke(mantissaSize: Int, exponentSize: Int): IEEE754Value {
        return IEEE754Value(mantissaSize, exponentSize, this)
    }

    fun test(value: IEEE754Value): Boolean {
        for (i in 0 until 1 + value.exponentSize + value.mantissaSize) {
            @Suppress("KotlinConstantConditions")
            val res = when {
                i >= 1 + value.exponentSize -> mantissa(i - 1 - value.exponentSize, value.mantissaSize)
                i >= 1 -> exponent(i - 1, value.exponentSize)
                i == 0 -> sign()
                else -> error("bad index $i")
            }
            if (value.getRaw(i) != res) return false
        }
        return true
    }
}

//fun main() {
//    println(IEEE754Value.fromDouble(8.75).toFloat())
//    println(IEEE754Value.fromFloat(28.7f).toDouble())
//    println(28.7f.toDouble())
//    DefaultFloatBound.values().forEach {
//        println(IEEE754Value(3, 3, it).toFloat())
//    }
//}