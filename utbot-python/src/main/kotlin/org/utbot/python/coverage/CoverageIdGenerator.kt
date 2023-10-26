package org.utbot.python.coverage

import java.util.concurrent.atomic.AtomicLong

object CoverageIdGenerator {
    private const val LOWER_BOUND: Long = 1500_000_000

    private val lastId: AtomicLong = AtomicLong(LOWER_BOUND)

    fun createId(): String {
        return lastId.incrementAndGet().toString(radix = 16)
    }
}
