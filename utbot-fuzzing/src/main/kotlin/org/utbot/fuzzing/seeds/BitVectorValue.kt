package org.utbot.fuzzing.seeds

import org.utbot.fuzzing.*
import org.utbot.fuzzing.utils.Endian
import java.math.BigInteger
import java.util.BitSet

class BitVectorValue : KnownValue {

    /**
     * Vector of value bits.
     *
     * Here, the base is 2 and the exponent for each member is equal to index of this member.
     * Therefore, the values is stored using LE system.
     */
    private val vector: BitSet
    val size: Int
    override val lastMutation: Mutation<KnownValue>?
    override val mutatedFrom: KnownValue?

    constructor(bits: Int, bound: Bound) {
        vector = BitSet(bits).also {
            for (i in 0 until bits) {
                it[i] = bound.initializer(i, bits)
            }
        }
        size = bits
        lastMutation = null
        mutatedFrom = null
    }

    constructor(other: BitVectorValue, mutation: Mutation<KnownValue>? = null) {
        vector = other.vector.clone() as BitSet
        size = other.size
        lastMutation = mutation
        mutatedFrom = other
    }

    private constructor(size: Int, value: BitSet) : this(size, { i, _ -> value[i] })

    operator fun get(index: Int): Boolean = vector[index]

    operator fun set(index: Int, value: Boolean) {
        vector[index] = value
    }

    /**
     * Increase value by 1.
     *
     * @return true if integer overflow is occurred in sign values
     */
    fun inc(): Boolean {
        var shift = 0
        var carry = true
        while (carry and (shift < size)) {
            if (!vector[shift]) {
                carry = false
            }
            vector[shift] = !vector[shift]
            shift++
        }
        return !carry && shift == size
    }

    /**
     * Decrease value by 1.
     *
     * @return true if integer underflow is occurred
     */
    fun dec(): Boolean {
        var shift = 0
        var carry = true
        while (carry and (shift < size)) {
            if (vector[shift]) {
                carry = false
            }
            vector[shift] = !vector[shift]
            shift++
        }
        return !carry && shift == size
    }

    override fun mutations() = listOf<Mutation<KnownValue>>(
        BitVectorMutations.SlightDifferent.adapt(),
        BitVectorMutations.DifferentWithSameSign.adapt(),
        BitVectorMutations.ChangeSign.adapt()
    )

    override fun equals(other: Any?): Boolean {
        if (other !is BitVectorValue) return false
        if (size != other.size) return false
        for (i in 0 until size) {
            if (vector[i] != other.vector[i]) return false
        }
        return true
    }

    override fun hashCode(): Int {
        return vector.hashCode()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun toString(radix: Int, isUnsigned: Boolean = false): String {
        val size = if (isUnsigned) size + 1 else size
        val array = ByteArray(size / 8 + if (size % 8 != 0) 1 else 0) { index ->
            toLong(bits = 8, shift = index * 8).toByte()
        }
        array.reverse()
        return BigInteger(array).toString(radix)
    }

    override fun toString() = toString(10)

    internal fun toBinaryString(endian: Endian) = buildString {
        for (i in endian.range(0, size - 1)) {
            append(if (this@BitVectorValue[i]) '1' else '0')
        }
    }

    private fun toLong(bits: Int, shift: Int = 0): Long {
        assert(bits <= 64) { "Cannot convert to long vector with more than 64 bits, but $bits is requested" }
        var result = 0L
        for (i in shift until minOf(bits + shift, size)) {
            result = result or ((if (vector[i]) 1L else 0L) shl (i - shift))
        }
        return result
    }

    private fun toBigInteger(bits: Int, shift: Int = 0): BigInteger {
        assert(bits <= 128) { "Cannot convert to long vector with more than 128 bits, but $bits is requested" }
        var result = BigInteger("0")
        for (i in shift until minOf(bits + shift, size)) {
            result = result or ((if (vector[i]) BigInteger("1") else BigInteger("0") shl (i - shift)))
        }
        return result
    }

    fun toBigInteger() = toBigInteger(128)

    fun toBoolean() = vector[0]

    fun toByte() = toLong(8).toByte()

    fun toUByte() = toLong(8).toUByte()

    fun toShort() = toLong(16).toShort()

    fun toUShort() = toLong(16).toUShort()

    fun toInt() = toLong(32).toInt()

    fun toUInt() = toLong(32).toUInt()

    fun toLong() = toLong(64)

    fun toULong() = toLong(64).toULong()

    fun toCharacter() = Char(toUShort())

    companion object {
        fun fromValue(value: Any): BitVectorValue {
            return when (value) {
                is Char -> fromChar(value)
                is Boolean -> fromBoolean(value)
                is Byte -> fromByte(value)
                is Short -> fromShort(value)
                is Int -> fromInt(value)
                is Long -> fromLong(value)
                else -> error("unknown type of value $value (${value::class})")
            }
        }

        fun  fromBoolean(value: Boolean): BitVectorValue {
            return BitVectorValue(1, if (value) Bool.TRUE else Bool.FALSE)
        }

        fun  fromByte(value: Byte): BitVectorValue {
            return fromLong(8, value.toLong())
        }

        fun  fromShort(value: Short): BitVectorValue {
            return fromLong(16, value.toLong())
        }

        fun  fromChar(value: Char): BitVectorValue {
            return fromLong(16, value.code.toLong())
        }

        fun  fromInt(value: Int): BitVectorValue {
            return fromLong(32, value.toLong())
        }

        fun  fromLong(value: Long): BitVectorValue {
            return fromLong(64, value)
        }

        private fun  fromLong(size: Int, value: Long): BitVectorValue {
            val vector = BitSet(size)
            for (i in 0 until size) {
                vector[i] = value and (1L shl i) != 0L
            }
            return BitVectorValue(size, vector)
        }

        fun  fromBigInteger(value: BigInteger): BitVectorValue {
            val size = 128
            val bits = value.bitCount()
            assert(bits <= size) { "This value $value is too big. Max value is 2^$bits." }
            val vector = BitSet(size)
            for (i in 0 until size) {
                vector[i] = value.testBit(i)
            }
            return BitVectorValue(size, vector)
        }
    }
}

fun interface Bound {
    fun initializer(index: Int, size: Int): Boolean
}

class DefaultBound private constructor(private val value: Long) : Bound {

    override fun initializer(index: Int, size: Int): Boolean {
        return value and (1L shl index) != 0L
    }

    @Suppress("unused")
    companion object {
        fun ofByte(value: Byte) = DefaultBound(value.toLong())

        fun ofUByte(value: UByte) = DefaultBound(value.toLong())

        fun ofShort(value: Short) = DefaultBound(value.toLong())

        fun ofUShort(value: UShort) = DefaultBound(value.toLong())

        fun ofInt(value: Int) = DefaultBound(value.toLong())

        fun ofUInt(value: UInt) = DefaultBound(value.toLong())

        fun ofLong(value: Long) = DefaultBound(value)

        fun ofULong(value: ULong) = DefaultBound(value.toLong())
    }
}

enum class Signed : Bound {
    ZERO { override fun initializer(index: Int, size: Int) = false },
    MIN { override fun initializer(index: Int, size: Int) = index == size - 1 },
    NEGATIVE { override fun initializer(index: Int, size: Int) = true },
    POSITIVE { override fun initializer(index: Int, size: Int) = index == 0 },
    MAX { override fun initializer(index: Int, size: Int) = index < size - 1 },
    ;

    operator fun invoke(size: Int) = BitVectorValue(size, this)

    fun test(value: BitVectorValue) = (0..value.size).all { value[it] == initializer(it, value.size) }
}

enum class Unsigned : Bound {
    ZERO { override fun initializer(index: Int, size: Int) = false },
    POSITIVE { override fun initializer(index: Int, size: Int) = index == 0 },
    MAX { override fun initializer(index: Int, size: Int) = true },
    ;

    operator fun invoke(size: Int) = BitVectorValue(size, this)
}

enum class Bool : Bound {
    FALSE { override fun initializer(index: Int, size: Int) = false },
    TRUE { override fun initializer(index: Int, size: Int) = true },
    ;

    operator fun invoke() = BitVectorValue(1, this)
}