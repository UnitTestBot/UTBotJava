package org.utbot.greyboxfuzzer.quickcheck.internal.generator

import org.utbot.greyboxfuzzer.quickcheck.internal.GeometricDistribution
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness

class SimpleGenerationStatus(
    distro: GeometricDistribution,
    random: SourceOfRandomness,
    private val attempts: Int
) : AbstractGenerationStatus(distro, random) {
    override fun attempts(): Int {
        return attempts
    }
}