package org.utbot.quickcheck.internal.generator

import org.utbot.quickcheck.internal.GeometricDistribution
import org.utbot.quickcheck.random.SourceOfRandomness

class SimpleGenerationStatus(
    distro: GeometricDistribution,
    random: SourceOfRandomness,
    private val attempts: Int
) : AbstractGenerationStatus(distro, random) {
    override fun attempts(): Int {
        return attempts
    }
}