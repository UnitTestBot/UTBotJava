package org.utbot.predictors

import org.utbot.analytics.UtBotAbstractPredictor

interface NNStateRewardPredictor : UtBotAbstractPredictor<List<Double>, Double>