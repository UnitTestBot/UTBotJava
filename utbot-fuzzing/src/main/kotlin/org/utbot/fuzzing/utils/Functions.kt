@file:Suppress("unused")

package org.utbot.fuzzing.utils

import org.utbot.fuzzing.seeds.BitVectorValue

internal fun <T> T.toBinaryString(
    size: Int,
    endian: Endian = Endian.BE,
    separator: String = " ",
    format: (index: Int) -> Boolean = BinaryFormat.BYTE,
    bit: (v: T, i: Int) -> Boolean
): String = buildString {
    (endian.range(0, size - 1)).forEachIndexed { index, i ->
        val b = if (bit(this@toBinaryString, i)) 1 else 0
        if (format(index)) append(separator)
        append(b)
    }
    appendLine()
}

internal fun UByte.toBinaryString(endian: Endian = Endian.BE, separator: String = " ", format: (index: Int) -> Boolean = BinaryFormat.BYTE): String {
    return toBinaryString(8, endian, separator, format) { v, i -> v.toInt() and (1 shl i) != 0 }
}

internal fun Byte.toBinaryString(endian: Endian = Endian.BE, separator: String = " ", format: (index: Int) -> Boolean = BinaryFormat.BYTE): String {
    return toBinaryString(8, endian, separator, format) { v, i -> v.toInt() and (1 shl i) != 0 }
}

internal fun UShort.toBinaryString(endian: Endian = Endian.BE, separator: String = " ", format: (index: Int) -> Boolean = BinaryFormat.BYTE): String {
    return toBinaryString(16, endian, separator, format) { v, i -> v.toInt() and (1 shl i) != 0 }
}

internal fun Short.toBinaryString(endian: Endian = Endian.BE, separator: String = " ", format: (index: Int) -> Boolean = BinaryFormat.BYTE): String {
    return toBinaryString(16, endian, separator, format) { v, i -> v.toInt() and (1 shl i) != 0 }
}

internal fun UInt.toBinaryString(endian: Endian = Endian.BE, separator: String = " ", format: (index: Int) -> Boolean = BinaryFormat.BYTE): String {
    return toBinaryString(32, endian, separator, format) { v, i -> v and (1u shl i) != 0u }
}

internal fun Int.toBinaryString(endian: Endian = Endian.BE, separator: String = " ", format: (index: Int) -> Boolean = BinaryFormat.BYTE): String {
    return toBinaryString(32, endian, separator, format) { v, i -> v and (1 shl i) != 0 }
}

internal fun ULong.toBinaryString(endian: Endian = Endian.BE, separator: String = " ", format: (index: Int) -> Boolean = BinaryFormat.BYTE): String {
    return toBinaryString(64, endian, separator, format) { v, i -> v and (1uL shl i) != 0uL }
}

internal fun Long.toBinaryString(endian: Endian = Endian.BE, separator: String = " ", format: (index: Int) -> Boolean = BinaryFormat.BYTE): String {
    return toBinaryString(64, endian, separator, format) { v, i -> v and (1L shl i) != 0L }
}

internal fun Float.toBinaryString(endian: Endian = Endian.BE, separator: String = " ", format: (index: Int) -> Boolean = {
    when (endian) {
        Endian.BE -> it == 1 || it == 9
        Endian.LE -> it == 31 || it == 23
    }
}): String {
    return toRawBits().toBinaryString(endian, separator, format)
}

internal fun Double.toBinaryString(endian: Endian = Endian.BE, separator: String = " ", format: (index: Int) -> Boolean = {
    when (endian) {
        Endian.BE -> it == 1 || it == 12
        Endian.LE -> it == 63 || it == 52
    }
}): String {
    return toRawBits().toBinaryString(endian, separator, format)
}

internal enum class Endian {
    BE { override fun range(fromInclusive: Int, toInclusive: Int) = (toInclusive downTo fromInclusive) },
    LE { override fun range(fromInclusive: Int, toInclusive: Int) = (fromInclusive .. toInclusive) };
    abstract fun range(fromInclusive: Int, toInclusive: Int): IntProgression
}

internal enum class BinaryFormat : (Int) -> Boolean {
    HALF { override fun invoke(index: Int) = index % 4 == 0 && index != 0 },
    BYTE { override fun invoke(index: Int) = index % 8 == 0 && index != 0 },
    DOUBLE { override fun invoke(index: Int) = index % 16 == 0 && index != 0 },
}

internal fun <T> MutableList<T>.transformIfNotEmpty(transform: MutableList<T>.() -> List<T>): List<T> {
    return if (isNotEmpty()) transform() else this
}

// todo move to tests
//fun main() {
//    val endian = Endian.BE
//    println(255.toUByte().toBinaryString(endian))
//    println(2.toBinaryString(endian))
//    println(BitVectorValue.fromInt(2).toBinaryString(endian))
//    print(8.75f.toBinaryString(endian))
//    print(8.75.toBinaryString(endian))
//}