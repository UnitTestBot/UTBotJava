package org.utbot.analytics

import org.utbot.engine.InterProceduralUnitGraph

/**
 * Class that can create FeatureProcessor. See [FeatureProcessor]
 */
interface FeatureProcessorFactory {
    operator fun invoke(graph: InterProceduralUnitGraph): FeatureProcessor
}