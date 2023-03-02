package org.utbot.python.newtyping.utils

import org.utbot.fuzzing.utils.chooseOne
import kotlin.random.Random

fun getOffsetLine(sourceFileContent: String, offset: Int): Int {
    return sourceFileContent.take(offset).count { it == '\n' } + 1
}

fun <T> weightedRandom(elems: List<T>, weights: List<Double>): T {
    val sum = weights.sum()
    val index = Random.chooseOne(weights.map { it / sum }.toDoubleArray())
    return elems[index]
}