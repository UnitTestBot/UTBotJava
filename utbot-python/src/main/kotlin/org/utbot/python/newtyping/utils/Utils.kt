package org.utbot.python.newtyping.utils

import kotlin.random.Random

fun getOffsetLine(sourceFileContent: String, offset: Int): Int {
    return sourceFileContent.take(offset).count { it == '\n' } + 1
}

fun <T> weightedRandom(elems: List<T>, weights: List<Double>): T {
    val sum = weights.sum()
    val borders = weights.fold(emptyList<Double>() to 0.0) { (list, partialSum), cur ->
        (list + (partialSum + cur) / sum) to partialSum + cur
    }.first
    val value = Random.nextDouble()
    return elems[borders.indexOfFirst { it >= value }]
}