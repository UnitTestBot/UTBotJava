package org.utbot.engine.greyboxfuzzer.quickcheck.internal.generator

import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.internal.GeometricDistribution
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness

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