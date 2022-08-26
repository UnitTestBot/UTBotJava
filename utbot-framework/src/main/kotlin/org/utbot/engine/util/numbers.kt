package org.utbot.engine.util

import kotlin.reflect.KClass


fun Boolean.toInt(): Int = if (this) 1 else 0
fun Int.toBoolean(): Boolean = this > 0
fun Number.toBoolean(): Boolean = toInt().toBoolean()

fun Number.recast(type: KClass<*>): Any = when (type) {
    Byte::class -> toByte()
    Short::class -> toShort()
    Int::class -> toInt()
    Long::class -> toLong()
    Float::class -> toFloat()
    Double::class -> toDouble()
    else -> throw IllegalStateException("Unsupported number type")
}

inline fun <reified T> Number.recast() = recast(T::class) as T

operator fun Number.plus(other: Number): Number = when (this) {
    is Long -> this.toLong() + other.toLong()
    is Int -> this.toInt() + other.toInt()
    is Short -> this.toShort() + other.toShort()
    is Byte -> this.toByte() + other.toByte()
    is Double -> this.toDouble() + other.toDouble()
    is Float -> this.toFloat() + other.toFloat()
    else -> error("Unknown numeric type")
}

operator fun Number.minus(other: Number): Number = when (this) {
    is Long -> this.toLong() - other.toLong()
    is Int -> this.toInt() - other.toInt()
    is Short -> this.toShort() - other.toShort()
    is Byte -> this.toByte() - other.toByte()
    is Double -> this.toDouble() - other.toDouble()
    is Float -> this.toFloat() - other.toFloat()
    else -> error("Unknown numeric type")
}

operator fun Number.times(other: Number): Number = when (this) {
    is Long -> this.toLong() * other.toLong()
    is Int -> this.toInt() * other.toInt()
    is Short -> this.toShort() * other.toShort()
    is Byte -> this.toByte() * other.toByte()
    is Double -> this.toDouble() * other.toDouble()
    is Float -> this.toFloat() * other.toFloat()
    else -> error("Unknown numeric type")
}

operator fun Number.div(other: Number): Number = when (this) {
    is Long -> this.toLong() / other.toLong()
    is Int -> this.toInt() / other.toInt()
    is Short -> this.toShort() / other.toShort()
    is Byte -> this.toByte() / other.toByte()
    is Double -> this.toDouble() / other.toDouble()
    is Float -> this.toFloat() / other.toFloat()
    else -> error("Unknown numeric type")
}

operator fun Number.rem(other: Number): Number = when (this) {
    is Long -> this.toLong() % other.toLong()
    is Int -> this.toInt() % other.toInt()
    is Short -> this.toShort() % other.toShort()
    is Byte -> this.toByte() % other.toByte()
    is Double -> this.toDouble() % other.toDouble()
    is Float -> this.toFloat() % other.toFloat()
    else -> error("Unknown numeric type")
}

operator fun Number.unaryMinus(): Number = when (this) {
    is Long -> this.toLong().unaryMinus()
    is Int -> this.toInt().unaryMinus()
    is Short -> this.toShort().unaryMinus()
    is Byte -> this.toByte().unaryMinus()
    is Double -> this.toDouble().unaryMinus()
    is Float -> this.toFloat().unaryMinus()
    else -> error("Unknown numeric type")
}

operator fun Number.compareTo(other: Number): Int = when (this) {
    is Long -> this.toLong().compareTo(other.toLong())
    is Int -> this.toInt().compareTo(other.toInt())
    is Short -> this.toShort().compareTo(other.toShort())
    is Byte -> this.toByte().compareTo(other.toByte())
    is Double -> this.toDouble().compareTo(other.toDouble())
    is Float -> this.toFloat().compareTo(other.toFloat())
    else -> error("Unknown numeric type")
}

fun Number.shl(bits: Int): Number = when (this) {
    is Long -> this.toLong().shl(bits)
    is Int -> this.toInt().shl(bits)
    is Short -> this.toShort().shl(bits)
    is Byte -> this.toByte().shl(bits)
    is Double -> this.toDouble().shl(bits)
    is Float -> this.toFloat().shl(bits)
    else -> error("Unknown numeric type")
}

fun Number.shr(bits: Int): Number = when (this) {
    is Long -> this.toLong().shr(bits)
    is Int -> this.toInt().shr(bits)
    is Short -> this.toShort().shr(bits)
    is Byte -> this.toByte().shr(bits)
    is Double -> this.toDouble().shr(bits)
    is Float -> this.toFloat().shr(bits)
    else -> error("Unknown numeric type")
}

fun Number.ushr(bits: Int): Number = when (this) {
    is Long -> this.toLong().ushr(bits)
    is Int -> this.toInt().ushr(bits)
    is Short -> this.toShort().ushr(bits)
    is Byte -> this.toByte().ushr(bits)
    is Double -> this.toDouble().ushr(bits)
    is Float -> this.toFloat().ushr(bits)
    else -> error("Unknown numeric type")
}

infix fun Number.and(other: Number): Number = when (this) {
    is Long -> this.toLong() and other.toLong()
    is Int -> this.toInt() and other.toInt()
    is Short -> this.toShort() and other.toShort()
    is Byte -> this.toByte() and other.toByte()
    is Double -> this.toDouble() and other.toDouble()
    is Float -> this.toFloat() and other.toFloat()
    else -> error("Unknown numeric type")
}

infix fun Number.or(other: Number): Number = when (this) {
    is Long -> this.toLong() or other.toLong()
    is Int -> this.toInt() or other.toInt()
    is Short -> this.toShort() or other.toShort()
    is Byte -> this.toByte() or other.toByte()
    is Double -> this.toDouble() or other.toDouble()
    is Float -> this.toFloat() or other.toFloat()
    else -> error("Unknown numeric type")
}

infix fun Number.xor(other: Number): Number = when (this) {
    is Long -> this.toLong() xor other.toLong()
    is Int -> this.toInt() xor other.toInt()
    is Short -> this.toShort() xor other.toShort()
    is Byte -> this.toByte() xor other.toByte()
    is Double -> this.toDouble() xor other.toDouble()
    is Float -> this.toFloat() xor other.toFloat()
    else -> error("Unknown numeric type")
}

fun Number.abs(): Number = when (this) {
    is Long -> this.abs()
    is Int -> this.abs()
    is Short -> this.abs()
    is Byte -> this.abs()
    is Double -> this.abs()
    is Float -> this.abs()
    else -> error("Unknown numeric type")
}