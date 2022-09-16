package org.utbot.analytics

/**
 * Encapsulates creation of [MLPredictor]
 */
interface MLPredictorFactory {
    operator fun invoke(): MLPredictor
}