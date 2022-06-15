package org.utbot.predictors

import org.utbot.analytics.UtBotAbstractPredictor

/**
 * Interface, which should predict reward for state by features list.
 */
interface NNStateRewardPredictor : UtBotAbstractPredictor<List<Double>, Double>