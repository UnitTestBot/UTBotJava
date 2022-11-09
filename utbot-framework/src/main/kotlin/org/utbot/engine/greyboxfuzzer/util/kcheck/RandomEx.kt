package org.utbot.engine.greyboxfuzzer.util.kcheck

import java.util.*

class HighQualityRandom(seed: Long = System.nanoTime()): Random(seed) {
    private var u: Long = 0L
    private var v = 4101842887655102017L
    private var w = 1L

    init {
        u = seed xor v;
        nextLong()
        v = u;
        nextLong()
        w = v;
        nextLong()
    }

    override fun nextLong(): Long {
        u = u * 2862933555777941757L + 7046029254386353087L;
        v = v xor (v ushr 17)
        v = v xor (v shl 31)
        v = v xor (v ushr 8)
        w = 4294957665L * (w and 0xffffffff) + (w ushr 32);
        var x = u xor (u shl 21);
        x = x xor (x ushr 35)
        x = x xor (x shl 4)
        return (x + v) xor w
    }

    override fun next(bits: Int): Int {
        return (nextLong() ushr (64 - bits)).toInt()
    }
}

fun Random.nextLong(bound: Long): Long {
    var bits: Long
    var v: Long
    do {
        bits = (nextLong() shl 1).ushr(1)
        v = bits % bound
    } while (bits - v + (bound - 1) < 0L)
    return v
}
fun Random.nextShort(): Short = nextInRange(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt() + 1).toShort()
fun Random.nextShort(bound: Short): Short = nextInt(bound.toInt()).toShort()
fun Random.nextByte(): Byte = nextInRange(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt() + 1).toByte()
fun Random.nextByte(bound: Byte): Byte = nextInt(bound.toInt()).toByte()
fun Random.nextChar(): Char = nextShort().toChar()
fun Random.nextChar(alphabet: CharSequence) = alphabet[nextInt(alphabet.length)]
fun Random.nextString(alphabet: CharSequence, minLength: Int = 0, maxLength: Int = 1001) =
        charSequence(alphabet).take(nextInRange(minLength, maxLength)).joinToString(separator = "")

fun Random.nextInRange(min: Int, max: Int): Int = when {
    max == min -> min
    else -> (nextLong(max.toLong() - min.toLong()) + min).toInt()
}


fun Random.nextInRange(min: Long, max: Long): Long {
    // this is a bit tricky
    val minHalf = min / 2
    val maxHalf = max / 2
    val minOtherHalf = min - minHalf
    val maxOtherHalf = max - maxHalf
    return nextLong(maxHalf - minHalf + 1) + nextLong(maxOtherHalf - minOtherHalf) + min
}

fun Random.nextInRange(min: Short, max: Short): Short = (nextInt(max - min) + min).toShort()
fun Random.nextInRange(min: Byte, max: Byte): Byte = (nextInt(max - min) + min).toByte()
fun Random.nextInRange(min: Float, max: Float): Float = nextFloat() * (max - min) + min
fun Random.nextInRange(min: Double, max: Double): Double = nextDouble() * (max - min) + min

fun Random.intSequence() = generateSequence { nextInt() }
fun Random.longSequence() = generateSequence { nextLong() }
fun Random.shortSequence() = generateSequence { nextShort() }
fun Random.byteSequence() = generateSequence { nextByte() }
fun Random.booleanSequence() = generateSequence { nextBoolean() }
fun Random.floatSequence() = generateSequence { nextFloat() }
fun Random.doubleSequence() = generateSequence { nextDouble() }
fun Random.charSequence(alphabet: CharSequence) = generateSequence { nextChar(alphabet) }
fun Random.stringSequence(alphabet: CharSequence, minLength: Int = 0, maxLength: Int = 1000) =
        generateSequence { nextString(alphabet, minLength, maxLength) }
