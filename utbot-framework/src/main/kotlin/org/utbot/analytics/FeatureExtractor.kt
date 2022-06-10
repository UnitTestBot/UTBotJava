package org.utbot.analytics

import org.utbot.engine.ExecutionState

/**
 * Class that encapsulates work with FeatureExtractor during symbolic execution.
 */
interface FeatureExtractor {
    /**
     * Extract features and store in it [ExecutionState.features]
     * @param generatedTestCases number of generated tests so far
     */
    fun extractFeatures(executionState: ExecutionState, generatedTestCases: Int)
}