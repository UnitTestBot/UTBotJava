package org.utbot.instrumentation.instrumentation.execution

import org.utbot.common.StopWatch
import org.utbot.common.ThreadBasedExecutor
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.api.util.withUtContext

fun <T> invokeWithTimeoutWithUtContext(timeoutMillis: Long, block: () -> T): Pair<T, Long> {
    val stopWatch = StopWatch()
    val context = UtContext(utContext.classLoader, stopWatch)
    val result = ThreadBasedExecutor.threadLocal.invokeWithTimeout(timeoutMillis, stopWatch) {
        withUtContext(context) {
            block()
        }
    } ?: throw TimeoutException("Timeout $timeoutMillis elapsed")

    val elapsedMillis = stopWatch.get()

    return Pair(result.getOrThrow() as T, elapsedMillis)
}