package org.utbot.engine.greyboxfuzzer.mutator

import org.utbot.engine.greyboxfuzzer.util.getTrue
import kotlin.math.abs
import kotlin.random.Random

class SeedCollector(private val maxSize: Int = 50, private val methodLines: Set<Int>) {
    private val seeds = ArrayList<Seed>(maxSize)
//        sortedSetOf(
//            comparator =
//            compareByDescending { seed: Seed -> seed.score }.thenComparator { seed1, seed2 -> if (seed1 === seed2) 0 else 1 }
//        )

    fun calcSeedScore(coverage: Set<Int>): Double =
        coverage.sumOf { line ->
            val numOfSeedCoveredLine = seeds.count { it.lineCoverage.contains(line) }
            if (numOfSeedCoveredLine == 0) {
                Double.MAX_VALUE
            } else {
                1.0 / numOfSeedCoveredLine
            }
        }

    private fun recalculateSeedScores() {
        seeds.forEach { seed ->
            seed.score = calcSeedScore(seed.lineCoverage)
        }
    }

    //
    fun addSeed(seed: Seed) {
        if (seeds.isEmpty()) {
            seeds.add(seed)
            return
        }
        val indexToInsert = seeds.indexOfFirst { it.score <= seed.score }
        if (indexToInsert == -1 && seeds.size < maxSize - 1) {
            seeds.add(seed)
            recalculateSeedScores()
            seeds.sortByDescending { it.score }
            return
        }
        seeds.add(indexToInsert, seed)
        recalculateSeedScores()
        seeds.sortByDescending { it.score }
        while (seeds.size >= maxSize) {
            seeds.removeLast()
        }
    }

    fun getRandomWeightedSeed(): Seed {
        val scoreSum = seeds.sumOf { it.score }
        val randomScore = Random.nextDouble(0.0, scoreSum)
        var scoreCounter = 0.0
        seeds.forEach { seed ->
            scoreCounter += seed.score
            if (scoreCounter >= randomScore) {
                return seed
            }
        }
        return seeds.first()
    }

    fun getBestSeed() = seeds.first()
    fun removeSeed(seed: Seed) = seeds.remove(seed)
    fun seedsSize() = seeds.size


}