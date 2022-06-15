package org.utbot.analytics

/**
 * Encapsulates creation of [StateRewardPredictor]
 */
interface StateRewardPredictorFactory {
    operator fun invoke(): StateRewardPredictor
}