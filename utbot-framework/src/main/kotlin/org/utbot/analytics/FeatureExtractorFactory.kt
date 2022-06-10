package org.utbot.analytics

import org.utbot.engine.InterProceduralUnitGraph

/**
 * Class that can create FeatureExtractor. See [FeatureExtractor].
 */
interface FeatureExtractorFactory {
    operator fun invoke(graph: InterProceduralUnitGraph): FeatureExtractor
}