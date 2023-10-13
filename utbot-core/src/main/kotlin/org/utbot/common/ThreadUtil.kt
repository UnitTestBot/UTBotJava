package org.utbot.common

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.properties.ReadOnlyProperty
import kotlin.random.Random
import kotlin.reflect.KProperty


class ThreadLocalLazy<T>(val provider: () -> T) : ReadOnlyProperty<Any?, T> {
    private val threadLocal = object : ThreadLocal<T>() {
        override fun initialValue(): T = provider()
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        threadLocal.get()
}
fun <T> threadLocalLazy(provider: () -> T) = ThreadLocalLazy(provider)



class ThreadBasedExecutor {
    companion object {
        val threadLocal by threadLocalLazy { ThreadBasedExecutor() }
    }

    /**
     * Used to avoid calling [Thread.stop] during clean up.
     *
     * @see runCleanUpIfTimedOut
     */
    private val timeOutCleanUpLock = ReentrantLock()

    /**
     * `null` when either:
     * - no tasks have yet been run
     * - current task timed out, and we are waiting for its thread to die
     */
    @Volatile
    private var thread: Thread? = null

    private var requestQueue = ArrayBlockingQueue<() -> Any?>(1)
    private var responseQueue = ArrayBlockingQueue<Result<Any?>>(1)

    /**
     * Can be called from lambda passed to [invokeWithTimeout].
     * [ThreadBasedExecutor] guarantees that it won't attempt to terminate [cleanUpBlock] with [Thread.stop].
     */
    fun runCleanUpIfTimedOut(cleanUpBlock: () -> Unit) {
        timeOutCleanUpLock.withLock {
            if (thread == null)
                cleanUpBlock()
        }
    }

    /**
     * Invoke [action] with timeout.
     *
     * [stopWatch] is used to respect specific situations (such as class loading and transforming) while invoking.
     */
    fun invokeWithTimeout(timeoutMillis: Long, stopWatch: StopWatch? = null, action:() -> Any?) : Result<Any?>? {
        ensureThreadIsAlive()

        requestQueue.offer {
            try {
                stopWatch?.start()
                action()
            } finally {
                stopWatch?.stop()
            }
        }

        var res = responseQueue.poll(timeoutMillis, TimeUnit.MILLISECONDS)
        if (res == null && stopWatch != null) {
            var millis = timeoutMillis - stopWatch.get(TimeUnit.MILLISECONDS)
            while (res == null && millis > 0) {
                res = responseQueue.poll(millis, TimeUnit.MILLISECONDS)
                millis = timeoutMillis - stopWatch.get(TimeUnit.MILLISECONDS)
            }
        }

        if (res == null) {
            try {
                val t = thread ?: return res
                thread = null
                t.interrupt()
                t.join(10)
                // to avoid race condition we need to wait for `t` to die
                while (t.isAlive) {
                    timeOutCleanUpLock.withLock {
                        @Suppress("DEPRECATION")
                        t.stop()
                    }
                    // If somebody catches `ThreadDeath`, for now we
                    // just wait for at most 10s and throw another one.
                    //
                    // A better approach may be to kill instrumented process.
                    t.join(10_000)
                }
            } catch (_: Throwable) {}
        }
        return res
    }

    fun invokeWithoutTimeout(action:() -> Any?) : Result<Any?> {
        ensureThreadIsAlive()

        requestQueue.offer(action)
        return responseQueue.take()
    }

    private fun ensureThreadIsAlive() {
        if (thread?.isAlive != true) {
            requestQueue = ArrayBlockingQueue<() -> Any?>(1)
            responseQueue = ArrayBlockingQueue<Result<Any?>>(1)

            thread = thread(name = "executor @${Random.nextInt(10_000)}", isDaemon = true) {
                try {
                    while (thread === Thread.currentThread()) {
                        val next = requestQueue.take()
                        responseQueue.offer(kotlin.runCatching { next() })
                    }
                } catch (_: InterruptedException) {}
            }
        }
    }
}