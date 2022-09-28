package org.utbot.rd

import com.jetbrains.rd.framework.util.RdCoroutineScope
import com.jetbrains.rd.framework.util.asCoroutineDispatcher
import com.jetbrains.rd.util.lifetime.Lifetime

class UtRdCoroutineScope(lifetime: Lifetime) : RdCoroutineScope(lifetime) {
    companion object {
        val current = UtRdCoroutineScope(Lifetime.Eternal)
    }

    init {
        override(lifetime, this)
    }

    override val defaultDispatcher = UtSingleThreadScheduler("UtCoroutineScheduler").asCoroutineDispatcher
}