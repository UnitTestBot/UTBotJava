package org.utbot.rd

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.threading.SingleThreadSchedulerBase

private val logger = getLogger<UtSingleThreadScheduler>()

class UtSingleThreadScheduler(name: String) : SingleThreadSchedulerBase(name) {
    override fun onException(ex: Throwable) {
        logger.error { "exception on scheduler $name: $ex |> ${ex.stackTraceToString()}" }
    }
}