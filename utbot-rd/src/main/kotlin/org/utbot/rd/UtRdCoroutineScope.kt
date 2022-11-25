package org.utbot.rd

import com.jetbrains.rd.framework.util.RdCoroutineScope
import com.jetbrains.rd.framework.util.asCoroutineDispatcher
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.threading.SingleThreadScheduler

private val coroutineDispatcher = SingleThreadScheduler(Lifetime.Eternal, "UtCoroutineScheduler").asCoroutineDispatcher

class UtRdCoroutineScope(lifetime: Lifetime) : RdCoroutineScope(lifetime) {
    companion object {
        val current = UtRdCoroutineScope(Lifetime.Eternal)
        fun initialize() {
            // only to load and initialize class
        }
    }

    init {
        override(lifetime, this)
    }

    override val defaultDispatcher = coroutineDispatcher
}