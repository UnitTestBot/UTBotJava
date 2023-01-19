package org.utbot.engine

import java.util.concurrent.atomic.AtomicInteger

/**
 * Counts new objects during execution. Used to give new addresses for objects in [Traverser] and [Resolver].
 */
data class ObjectCounter(val initialValue: Int) {
    private val internalCounter = AtomicInteger(initialValue)

    fun createNewAddr(): Int = internalCounter.getAndIncrement()
}
