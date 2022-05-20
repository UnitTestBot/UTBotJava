package org.utbot.common

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Threadsafe stopwatch.
 *
 * Using for respect class transforming time while invoking with timeout.
 *
 * Transforming can start in random moment while invoking.
 * Example: - is invoking, _ is transforming
 *
 * ----- _______ --- __ - __ ----
 *
 * To check current state of [elapsedMillis] in main thread we need to understand stopwatch is running or not and
 * when it started, also state can changed while invoking in other thread, so it can be implemented by locking mechanism.
 *
 */
class StopWatch {
    private val lock = ReentrantLock()
    
    private var elapsedMillis: Long = 0
    private var startTime: Long? = null

    fun start() {
        lock.withLockInterruptibly {
            startTime = System.currentTimeMillis()
        }
    }
    
    fun stop() {
        lock.withLockInterruptibly {
            startTime?.let {
                elapsedMillis += (System.currentTimeMillis() - it)
                startTime = null
            }
        }
    }

    private fun unsafeUpdate() {
        startTime?.let {
            val current = System.currentTimeMillis()
            elapsedMillis += (current - it)
            startTime = current
        }
    }
    
    fun loop() {
        lock.withLockInterruptibly {
            unsafeUpdate()
        }
    }

    fun get(unit: TimeUnit) = lock.withLockInterruptibly {
        unsafeUpdate()
        unit.convert(elapsedMillis, TimeUnit.MILLISECONDS)
    }
    
}