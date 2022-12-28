package org.utbot.fuzzing.utils

import kotlin.jvm.Throws
import kotlin.random.Random

/**
 * Creates iterable for all values of cartesian product of `lists`.
 */
class CartesianProduct<T>(
    private val lists: List<List<T>>,
    private val random: Random? = null
): Iterable<List<T>> {

    /**
     * Estimated number of all combinations.
     */
    val estimatedSize: Long
        get() = Combinations(*lists.map { it.size }.toIntArray()).size

    @Throws(TooManyCombinationsException::class)
    fun asSequence(): Sequence<List<T>> {
        val combinations = Combinations(*lists.map { it.size }.toIntArray())
        val sequence = if (random != null) {
            sequence {
                forEachChunk(Int.MAX_VALUE, combinations.size) { startIndex, combinationSize, _ ->
                    val permutation = PseudoShuffledIntProgression(combinationSize, random)
                    val temp = IntArray(size = lists.size)
                    for (it in 0 until combinationSize) {
                        yield(combinations[permutation[it] + startIndex, temp])
                    }
                }
            }
        } else {
            combinations.asSequence()
        }
        return sequence.map { combination ->
            combination.mapIndexedTo(ArrayList(combination.size)) { index, value -> lists[index][value] }
        }
    }

    override fun iterator(): Iterator<List<T>> = asSequence().iterator()

    companion object {
        /**
         * Consumer for processing blocks of input larger block.
         *
         * If source example is sized to 12 and every block is sized to 5 then consumer should be called 3 times with these values:
         *
         * 1. start = 0, size = 5, remain = 7
         * 2. start = 5, size = 5, remain = 2
         * 3. start = 10, size = 2, remain = 0
         *
         * The sum of start, size and remain should be equal to source block size.
         */
        internal inline fun forEachChunk(
            chunkSize: Int,
            totalSize: Long,
            block: (start: Long, size: Int, remain: Long) -> Unit
        ) {
            val iterationsCount = totalSize / chunkSize + if (totalSize % chunkSize == 0L) 0 else 1
            (0L until iterationsCount).forEach { iteration ->
                val start = iteration * chunkSize
                val size = minOf(chunkSize.toLong(), totalSize - start).toInt()
                val remain = totalSize - size - start
                block(start, size, remain)
            }
        }
    }
}

inline fun <T> List<List<T>>.cartesian(block: (List<T>) -> Unit) {
    cartesian().forEach(block)
}

fun <T> List<List<T>>.cartesian(): Sequence<List<T>> = sequence {
    cartesian(this@cartesian, 0, IntArray(size))
}

private suspend fun <T> SequenceScope<List<T>>.cartesian(lists: List<List<T>>, iteration: Int, array: IntArray) {
    if (iteration == lists.size) {
        yield(array.mapIndexed { l, v -> lists[l][v] })
    } else {
        check(iteration < lists.size)
        for (j in lists[iteration].indices) {
            array[iteration] = j
            cartesian(lists, iteration + 1, array)
        }
    }
}

//private suspend fun <T> SequenceScope<List<T>>.cartesian(lists: List<List<T>>, head: List<T>) {
//    if (head.size == lists.size) {
//        yield(head)
//    } else {
//        check(head.size < lists.size)
//        lists[head.size].forEach {
//            val copy = head + it
//            cartesian(lists, copy)
//        }
//    }
//}