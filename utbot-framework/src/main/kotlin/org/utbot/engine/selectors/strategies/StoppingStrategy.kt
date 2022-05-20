package org.utbot.engine.selectors.strategies

interface StoppingStrategy {
    fun shouldStop(): Boolean
}
