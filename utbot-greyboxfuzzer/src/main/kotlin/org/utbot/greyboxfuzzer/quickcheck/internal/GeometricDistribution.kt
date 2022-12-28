package org.utbot.greyboxfuzzer.quickcheck.internal

import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import kotlin.math.ceil
import kotlin.math.ln

class GeometricDistribution {
    fun sampleWithMean(mean: Double, random: SourceOfRandomness): Int {
        return sample(probabilityOfMean(mean), random)
    }

    fun sample(p: Double, random: SourceOfRandomness): Int {
        ensureProbability(p)
        if (p == 1.0) return 0
        val uniform = random.nextDouble()
        return ceil(ln(1 - uniform) / ln(1 - p)).toInt()
    }

    fun probabilityOfMean(mean: Double): Double {
        require(mean > 0) { "Need a positive mean, got $mean" }
        return 1 / mean
    }

    private fun ensureProbability(p: Double) {
        require(!(p <= 0 || p > 1)) { "Need a probability in (0, 1], got $p" }
    }
}