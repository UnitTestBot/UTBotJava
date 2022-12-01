package org.utbot.engine.greyboxfuzzer.quickcheck.internal

import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness

object Items {
    @JvmStatic
    fun <T> choose(items: Collection<T>, random: SourceOfRandomness): T {
        val size = items.size
        require(size != 0) { "Collection is empty, can't pick an element from it" }
        if (items is RandomAccess && items is List<*>) {
            val list = items as List<T>
            return if (size == 1) list[0] else list[random.nextInt(size)]
        }
        if (size == 1) {
            return items.iterator().next()
        }
        return items.toList()[random.nextInt(items.size)]
    }

    fun <T> chooseWeighted(
        items: Collection<Weighted<T>>,
        random: SourceOfRandomness
    ): T {
        if (items.size == 1) return items.iterator().next().item
        val range = items.stream().mapToInt { i: Weighted<T> -> i.weight }.sum()
        val sample = random.nextInt(range)
        var threshold = 0
        for (each in items) {
            threshold += each.weight
            if (sample < threshold) return each.item
        }
        throw AssertionError(String.format("sample = %d, range = %d", sample, range))
    }
}