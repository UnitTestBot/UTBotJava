package org.utbot.analytics

import org.utbot.engine.ExecutionState

/**
 * Class that incapsulates work with FeatureExtractor during symbolic execution
 */
interface FeatureExtractor {
    fun extractFeatures(executionState: ExecutionState, generatedTestCases: Int)
}