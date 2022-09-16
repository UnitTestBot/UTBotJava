package org.utbot.analytics

/**
 * Interface, which should predict reward for state by features list.
 */
interface MLPredictor : UtBotAbstractPredictor<List<Double>, Double>