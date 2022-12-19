package org.utbot.engine.greyboxfuzzer.mutator

import kotlin.random.Random

class SeedCollector(private val maxSize: Int = 50, private val methodInstructionsIds: Set<Long>) {
    private val seeds = ArrayList<Seed>(maxSize)

    fun calcSeedScore(coverage: Set<Long>): Double =
        coverage.sumOf { instruction ->
            val numOfSeedCoveredInstructions = seeds.count { it.instructionCoverage.contains(instruction) }
            if (numOfSeedCoveredInstructions == 0) {
                Double.MAX_VALUE
            } else {
                1.0 / numOfSeedCoveredInstructions
            }
        }

    private fun recalculateSeedScores() {
        seeds.forEach { seed ->
            seed.score = calcSeedScore(seed.instructionCoverage)
        }
    }

    fun isSeedOpensNewCoverage(seed: Seed): Boolean {
        val oldCoverage = seeds.flatMap { it.instructionCoverage }.toSet()
        return seed.instructionCoverage.any { !oldCoverage.contains(it) }
    }

    fun addSeed(seed: Seed) {
        if (seeds.isEmpty()) {
            seeds.add(seed)
            return
        }
        val indexToInsert = seeds.indexOfFirst { it.score <= seed.score }
        if (indexToInsert == -1) {
            if (seeds.size < maxSize - 1) {
                seeds.add(seed)
                recalculateSeedScores()
                seeds.sortByDescending { it.score }
            }
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
        val topN = when (Random.nextInt(0, 100)) {
            in 0..30 -> 1
            in 30..60 -> 3
            in 60..80 -> 5
            in 80..90 -> 10
            else -> maxSize
        }
        return seeds.take(topN).random()
    }

    fun all(f: (Seed) -> Boolean) = seeds.all(f)

//    fun getRandomWeightedSeed(): Seed {
//        val scoreSum = seeds.sumOf { it.score }
//        val randomScore = Random.nextDouble(0.0, scoreSum)
//        var scoreCounter = 0.0
//        seeds.forEach { seed ->
//            scoreCounter += seed.score
//            if (scoreCounter >= randomScore) {
//                return seed
//            }
//        }
//        return seeds.first()
//    }

    fun getBestSeed() = seeds.first()
    fun removeSeed(seed: Seed) = seeds.remove(seed)
    fun seedsSize() = seeds.size


}