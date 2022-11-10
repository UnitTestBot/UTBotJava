package org.utbot.python.utils

import java.lang.Integer.min

class PriorityCartesianProduct<T>(private val lists: List<List<T>>) {

    private fun generateFixedSumRepresentation(
        sum: Int,
        index: Int = 0,
        curRepr: List<Int> = emptyList()
    ): Sequence<List<Int>> {
        val itemNumber = lists.size
        var result = emptySequence<List<Int>>()
        if (index == itemNumber && sum == 0) {
            return sequenceOf(curRepr)
        } else if (index < itemNumber && sum >= 0) {
            for (i in 0..min(sum, lists[index].size - 1)) {
                result += generateFixedSumRepresentation(
                    sum - i,
                    index + 1,
                    curRepr + listOf(i)
                )
            }
        }
        return result
    }

    fun getSequence(): Sequence<List<T>> {
        var curSum = 0
        val maxSum = lists.fold(0) { acc, elem -> acc + elem.size }
        val combinations = generateSequence {
            if (curSum > maxSum)
                null
            else
                generateFixedSumRepresentation(curSum++)
        }
        return combinations.flatten().map { combination: List<Int> ->
            combination.mapIndexed { element, value -> lists[element][value] }
        }
    }
}
