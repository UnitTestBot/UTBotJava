package org.utbot.features

import org.utbot.analytics.FeatureExtractor
import org.utbot.analytics.FeatureExtractorFactory
import org.utbot.engine.InterProceduralUnitGraph

/**
 * Implementation of feature extractor factory
 */
class FeatureExtractorFactoryImpl : FeatureExtractorFactory {
    override operator fun invoke(graph: InterProceduralUnitGraph): FeatureExtractor = FeatureExtractorImpl(graph)
}