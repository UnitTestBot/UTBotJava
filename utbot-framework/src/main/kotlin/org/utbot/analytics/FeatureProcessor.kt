package org.utbot.analytics

import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.selectors.strategies.TraverseGraphStatistics

/**
 * Interface that encapsulates work with FeatureProcessor and can only dumpFeatures at the end of symbolic execution
 */
abstract class FeatureProcessor(graph: InterProceduralUnitGraph) : TraverseGraphStatistics(graph) {
    /**
     * Dump features and rewards of all states in collected test cases.
     */
    abstract fun dumpFeatures()
}
