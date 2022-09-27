package org.utbot.kotlin.examples.strings

fun factorial(n: Int): Int {
    require(n >= 0)
    return if (n == 0) {
        1
    } else n * factorial(n - 1)
}

fun fib(n: Int): Int {
    require(n >= 0)
    return when (n) {
        0 -> 0
        1 -> 1
        else -> fib(n - 1) + fib(n - 2)
    }
}