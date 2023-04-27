package org.utbot.python.evaluation.utils

import java.util.concurrent.atomic.AtomicLong

object CoverageIdGenerator {
    private const val lower_bound: Long = 1500_000_000

    private val lastId: AtomicLong = AtomicLong(lower_bound)

    fun createId(): String {
        return lastId.incrementAndGet().toString(radix = 16)
    }
}
