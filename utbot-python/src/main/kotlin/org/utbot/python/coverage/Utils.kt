package org.utbot.python.coverage

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

fun Long.toPair(): Pair<Long, Long> {
    val n = ceil(sqrt(this + 2.0)).toLong() - 1
    val k = this - (n * n - 1)
    return if (k <= n + 1) {
        n + 1 to k
    } else {
        k to n + 1
    }
}

fun Pair<Long, Long>.toCoverageId(): Long {
    val n = max(this.first, this.second) - 1
    val k = min(this.first, this.second)
    return (n * n - 1) + k
}