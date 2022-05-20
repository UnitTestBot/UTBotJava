package org.utbot.common

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.properties.ReadOnlyProperty
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

    private var thread: Thread? = null

    private var requestQueue = ArrayBlockingQueue<() -> Any?>(1)
    private var responseQueue = ArrayBlockingQueue<Result<Any?>>(1)


    /**
     * Invoke [action] with timeout.
     *
     * [stopWatch] is used to respect specific situations (such as class loading and transforming) while invoking.
     */
    fun invokeWithTimeout(timeoutMillis: Long, stopWatch: StopWatch? = null, action:() -> Any?) : Result<Any?>? {
        if (thread?.isAlive != true) {
            requestQueue = ArrayBlockingQueue<() -> Any?>(1)
            responseQueue = ArrayBlockingQueue<Result<Any?>>(1)

            thread = thread(name = "executor", isDaemon = true) {
                try {
                    while (true) {
                        val next = requestQueue.take()
                        responseQueue.offer(kotlin.runCatching { next() })
                    }
                } catch (_: InterruptedException) {}
            }
        }

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
                t.interrupt()
                t.join(10)
                if (t.isAlive)
                    @Suppress("DEPRECATION")
                    t.stop()
            } catch (_: Throwable) {}

            thread = null

        }
        return res
    }
}