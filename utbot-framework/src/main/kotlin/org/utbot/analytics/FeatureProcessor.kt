package org.utbot.analytics

import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.selectors.strategies.TraverseGraphStatistics

/**
 * Interface that incapsulates work with FeatureProcessor and can only dumpFeatures at the end of symbolic execution
 */
abstract class FeatureProcessor(graph: InterProceduralUnitGraph) : TraverseGraphStatistics(graph) {
    abstract fun dumpFeatures()
}
