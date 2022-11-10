package org.utbot.quickcheck.internal.generator

import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.internal.GeometricDistribution
import org.utbot.quickcheck.random.SourceOfRandomness

abstract class AbstractGenerationStatus(
    private val distro: GeometricDistribution,
    private val random: SourceOfRandomness
) : GenerationStatus {
    override fun size(): Int {
        return distro.sampleWithMean((attempts() + 1).toDouble(), random)
    }

    protected fun random(): SourceOfRandomness {
        return random
    }
}