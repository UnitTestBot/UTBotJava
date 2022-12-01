package org.utbot.engine.greyboxfuzzer.quickcheck

import org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.engine.greyboxfuzzer.quickcheck.internal.GeometricDistribution
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness

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
        return geometric.sampleWithMean(org.utbot.engine.greyboxfuzzer.quickcheck.NonTrackingGenerationStatus.Companion.MEAN_SIZE.toDouble(), random)
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