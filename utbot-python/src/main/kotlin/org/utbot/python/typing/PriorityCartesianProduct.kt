package org.utbot.python.typing

import org.utbot.fuzzer.CartesianProduct
import org.utbot.fuzzer.Combinations
import org.utbot.fuzzer.PseudoShuffledIntProgression
import kotlin.math.min
import kotlin.random.Random

fun main() {
    val x = PriorityCartesianProduct(listOf(listOf(1, 2, 3), listOf(4, 5), listOf(6, 7, 8, 9, 10)))
    x.forEach {
        println("-- $it")
    }
}

class PriorityCartesianProduct<T>(
    private val lists: List<List<T>>,
): Iterable<List<T>> {

    fun asSequence(): Sequence<List<T>> = iterator().asSequence()

    override fun iterator(): Iterator<List<T>> {
        val sizes = lists.map {it.size}
        val combinations = Combinations(*sizes.toIntArray())
        val groupedCombinations = combinations.groupBy { combination ->
            combination.sum()
        }
        val sortedCombinations = (0..sizes.sumOf { it - 1 }).mapNotNull {
            groupedCombinations[it]
        }
//        val groupedCombinations = combinations.groupBy { combination ->
//            combination.maxOf {it}
//        }
//        val sortedCombinations = (0..sizes.maxOf { it - 1 }).mapNotNull {
//            groupedCombinations[it]
//        }
        val sequence = sortedCombinations.flatten().asSequence()
        return sequence.map { combination ->
            combination.mapIndexedTo(mutableListOf()) { element, value -> lists[element][value] }
        }.iterator()
    }
}
