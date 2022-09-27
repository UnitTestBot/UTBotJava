package org.utbot.kotlin.examples.loops

fun whileLoop(k: Int): Int {
    var i = 0
    var sum = 0
    while (i < k) {
        sum += i
        i += 1
    }
    return sum
}