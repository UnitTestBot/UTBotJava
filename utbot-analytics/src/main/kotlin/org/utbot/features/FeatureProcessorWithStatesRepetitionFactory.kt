package org.utbot.features

import org.utbot.analytics.FeatureProcessor
import org.utbot.analytics.FeatureProcessorFactory
import org.utbot.engine.InterProceduralUnitGraph

/**
 * Implementation of feature processor factory, which creates FeatureProcessorWithStatesRepetition
 */
class FeatureProcessorWithStatesRepetitionFactory : FeatureProcessorFactory {
    override fun invoke(graph: InterProceduralUnitGraph): FeatureProcessor = FeatureProcessorWithStatesRepetition(graph)
}