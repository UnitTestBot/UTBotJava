package org.utbot.analytics

/**
 * Interface, which should predict reward for state by features list.
 */
interface StateRewardPredictor : UtBotAbstractPredictor<List<Double>, Double>