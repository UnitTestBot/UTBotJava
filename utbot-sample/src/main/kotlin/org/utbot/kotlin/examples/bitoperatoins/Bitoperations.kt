package org.utbot.kotlin.examples.bitoperatoins

fun xor(x: Int, y: Int): Boolean {
    return x xor y == 0
}

fun and(x: Int): Boolean {
    return x and x - 1 == 0
}

fun booleanNot(a: Boolean, b: Boolean): Int {
    val d = a && b
    val e = !a || b
    return if (d && e) 100 else 200
}

fun shl(x: Int): Boolean {
    return x shl 1 == 2
}

fun complement(x: Int): Boolean {
    return x.inv() == 1
}

fun shlWithBigLongShift(shift: Long): Int {
    if (shift < 40) {
        return 1
    }
    return if (0x77777777 shl shift.toInt() == 0x77777770) 2 else 3
}