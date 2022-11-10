package org.utbot.quickcheck

import org.utbot.quickcheck.generator.GenerationStatus
import org.utbot.quickcheck.internal.GeometricDistribution
import org.utbot.quickcheck.random.SourceOfRandomness

/**
 * Provides a generation status that does not track the number of trials
 * generated so far. This is useful for guided fuzzing where the burden
 * of making choices is on the guidance system rather than on quickcheck.
 *
 * @author Rohan Padhye
 */
class NonTrackingGenerationStatus(private val random: SourceOfRandomness) : GenerationStatus {
    private val geometric = GeometricDistribution()
    override fun size(): Int {
        return geometric.sampleWithMean(MEAN_SIZE.toDouble(), random)
    }

    override fun attempts(): Int {
        throw UnsupportedOperationException(
            "attempts() and @ValueOf" +
                    " is not supported in guided mode."
        )
    }

    companion object {
        const val MEAN_SIZE = 10
    }
}