package org.utbot.fuzzing.providers.known

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.DefaultBound
import org.utbot.fuzzing.seeds.Signed
import org.utbot.fuzzing.seeds.Unsigned
import kotlin.random.Random

class BitVectorValueTest {

    @Test
    fun `convert default kotlin literals to vector`() {
        for (i in Byte.MIN_VALUE..Byte.MAX_VALUE) {
            assertEquals(i.toByte(), BitVectorValue(Byte.SIZE_BITS, DefaultBound.ofByte(i.toByte())).toByte())
        }

        for (i in Short.MIN_VALUE..Short.MAX_VALUE) {
            assertEquals(i.toShort(), BitVectorValue(Short.SIZE_BITS, DefaultBound.ofShort(i.toShort())).toShort())
        }

        val randomIntSequence = with(Random(0)) { sequence { while (true) yield(nextInt()) } }
        randomIntSequence.take(100_000).forEach {
            assertEquals(it, BitVectorValue(Int.SIZE_BITS, DefaultBound.ofInt(it)).toInt())
        }

        val randomLongSequence = with(Random(0)) { sequence { while (true) yield(nextLong()) } }
        randomLongSequence.take(100_000).forEach {
            assertEquals(it, BitVectorValue(Long.SIZE_BITS, DefaultBound.ofLong(it)).toLong())
        }
    }

    @Test
    fun `test default bit vectors (byte)`() {
        assertEquals(0, BitVectorValue(8, Signed.ZERO).toByte())
        assertEquals(-128, BitVectorValue(8, Signed.MIN).toByte())
        assertEquals(-1, BitVectorValue(8, Signed.NEGATIVE).toByte())
        assertEquals(1, BitVectorValue(8, Signed.POSITIVE).toByte())
        assertEquals(127, BitVectorValue(8, Signed.MAX).toByte())
    }

    @Test
    fun `test default bit vectors (unsigned byte)`() {
        assertEquals(0u.toUByte(), BitVectorValue(8, Unsigned.ZERO).toUByte())
        assertEquals(1u.toUByte(), BitVectorValue(8, Unsigned.POSITIVE).toUByte())
        assertEquals(255u.toUByte(), BitVectorValue(8, Unsigned.MAX).toUByte())
    }

    @Test
    fun `test default bit vectors (short)`() {
        assertEquals(0, BitVectorValue(16, Signed.ZERO).toShort())
        assertEquals(Short.MIN_VALUE, BitVectorValue(16, Signed.MIN).toShort())
        assertEquals(-1, BitVectorValue(16, Signed.NEGATIVE).toShort())
        assertEquals(1, BitVectorValue(16, Signed.POSITIVE).toShort())
        assertEquals(Short.MAX_VALUE, BitVectorValue(16, Signed.MAX).toShort())
    }

    @Test
    fun `test default bit vectors (unsigned short)`() {
        assertEquals(UShort.MIN_VALUE, BitVectorValue(16, Unsigned.ZERO).toUShort())
        assertEquals(1u.toUShort(), BitVectorValue(16, Unsigned.POSITIVE).toUShort())
        assertEquals(UShort.MAX_VALUE, BitVectorValue(16, Unsigned.MAX).toUShort())
    }

    @Test
    fun `test default bit vectors (int)`() {
        assertEquals(0, BitVectorValue(32, Signed.ZERO).toInt())
        assertEquals(Int.MIN_VALUE, BitVectorValue(32, Signed.MIN).toInt())
        assertEquals(-1, BitVectorValue(32, Signed.NEGATIVE).toInt())
        assertEquals(1, BitVectorValue(32, Signed.POSITIVE).toInt())
        assertEquals(Int.MAX_VALUE, BitVectorValue(32, Signed.MAX).toInt())
    }

    @Test
    fun `test default bit vectors (unsigned int)`() {
        assertEquals(UInt.MIN_VALUE, BitVectorValue(32, Unsigned.ZERO).toUInt())
        assertEquals(1u, BitVectorValue(32, Unsigned.POSITIVE).toUInt())
        assertEquals(UInt.MAX_VALUE, BitVectorValue(32, Unsigned.MAX).toUInt())
    }

    @Test
    fun `test default bit vectors (long)`() {
        assertEquals(0, BitVectorValue(64, Signed.ZERO).toLong())
        assertEquals(Long.MIN_VALUE, BitVectorValue(64, Signed.MIN).toLong())
        assertEquals(-1, BitVectorValue(64, Signed.NEGATIVE).toLong())
        assertEquals(1, BitVectorValue(64, Signed.POSITIVE).toLong())
        assertEquals(Long.MAX_VALUE, BitVectorValue(64, Signed.MAX).toLong())
    }

    @Test
    fun `test default bit vectors (unsigned long)`() {
        assertEquals(ULong.MIN_VALUE, BitVectorValue(64, Unsigned.ZERO).toULong())
        assertEquals(1uL, BitVectorValue(64, Unsigned.POSITIVE).toULong())
        assertEquals(ULong.MAX_VALUE, BitVectorValue(64, Unsigned.MAX).toULong())
    }

    @Test
    fun `convert byte from and to`() {
        for (i in Byte.MIN_VALUE..Byte.MAX_VALUE) {
            assertEquals(i.toByte(), BitVectorValue.fromByte(i.toByte()).toByte())
        }
    }

    @Test
    fun `inc and dec byte`() {
        for (i in Byte.MIN_VALUE..Byte.MAX_VALUE) {
            val v = BitVectorValue.fromByte(i.toByte())
            assertEquals(i.toByte() == Byte.MAX_VALUE, v.inc()) { "$v" }
            assertEquals((i + 1).toByte(), v.toByte())
        }

        for (i in Byte.MAX_VALUE downTo Byte.MIN_VALUE) {
            val v = BitVectorValue.fromByte(i.toByte())
            assertEquals(i.toByte() == Byte.MIN_VALUE, v.dec()) { "$v" }
            assertEquals((i - 1).toByte(), v.toByte())
        }
    }

    @Test
    fun `inc and dec long`() {
        val r = with(Random(0)) { LongArray(1024) { nextLong() } }
        r[0] = Long.MIN_VALUE
        r[1] = Long.MAX_VALUE
        r.forEach { l ->
            val v = BitVectorValue.fromLong(l)
            assertEquals(l == Long.MAX_VALUE, v.inc()) { "$v" }
            assertEquals(l + 1, v.toLong())
        }
        r.forEach { l ->
            val v = BitVectorValue.fromLong(l)
            assertEquals(l == Long.MIN_VALUE, v.dec()) { "$v" }
            assertEquals(l - 1, v.toLong())
        }
    }
}