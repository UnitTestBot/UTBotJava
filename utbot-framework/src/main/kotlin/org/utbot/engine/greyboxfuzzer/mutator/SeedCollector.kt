package org.utbot.engine.greyboxfuzzer.mutator

import org.utbot.engine.greyboxfuzzer.util.getTrue
import java.util.PriorityQueue
import java.util.TreeSet
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.random.Random

class SeedCollector(private val maxSize: Int = 50) {
    private val seeds = sortedSetOf(
        comparator =
        compareByDescending { seed: Seed -> seed.priority }.thenComparator { seed1, seed2 -> if (seed1 === seed2) 0 else 1 }
    )

    fun addSeed(seed: Seed) {
        seeds.add(seed)
        while (seeds.size >= maxSize) {
            seeds.remove(seeds.last())
        }
    }

//    fun getRandomWeightedSeed(): Seed {
//        val priorityWeightedFunction = { a: Double -> exp(a).pow(2) }
//        val minPriority = seeds.last().priority
//        val maxPriority = seeds.first().priority
//        val priorityDiffs = if (maxPriority == minPriority) 1.0 else maxPriority - minPriority
//        val normalizedSeeds = seeds.map { it to (it.priority - minPriority) / priorityDiffs }
//        val sumOfSeedPriorities = normalizedSeeds.sumOf { priorityWeightedFunction.invoke(it.second) }
//        val randomSeedPriority = Random.nextDouble(0.0, sumOfSeedPriorities)
//        var priorityCounter = 0.0
//        normalizedSeeds.forEach { (seed, priority) ->
//            priorityCounter += priorityWeightedFunction.invoke(priority)
//            if (priorityCounter >= randomSeedPriority) {
//                return seed
//            }
//        }
//        return seeds.first()
//    }

    fun getRandomWeightedSeed() =
        if (Random.getTrue(75)) {
            val bestSeed = getBestSeed().priority
            seeds.filter { abs(it.priority - bestSeed) < 1e-5 }.randomOrNull()
        } else {
            seeds.randomOrNull()
        }

    fun getBestSeed() = seeds.first()
    fun removeSeed(seed: Seed) = seeds.remove(seed)
    fun seedsSize() = seeds.size


}