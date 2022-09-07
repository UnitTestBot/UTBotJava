package org.utbot.rd

import com.jetbrains.rd.util.threading.SingleThreadSchedulerBase

class UtSingleThreadScheduler(name: String = "UtRdScheduler", private val log: (() -> String) -> Unit) :
    SingleThreadSchedulerBase(name) {
    override fun onException(ex: Throwable) {
        log { ex.toString() }
    }
}