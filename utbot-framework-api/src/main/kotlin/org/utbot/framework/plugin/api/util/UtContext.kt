package org.utbot.framework.plugin.api.util

import org.utbot.common.StopWatch
import org.utbot.common.currentThreadInfo
import org.utbot.framework.plugin.api.util.UtContext.Companion.setUtContext
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement
import mu.KotlinLogging

val utContext: UtContext
    get() = UtContext.currentContext()
        ?: error("No context is set. Please use `withUtContext() {...}` or `setUtContext().use {...}`. Thread: ${currentThreadInfo()}")


class UtContext(val classLoader: ClassLoader) : ThreadContextElement<UtContext?> {

    // This StopWatch is used to respect bytecode transforming time while invoking with timeout
    var stopWatch: StopWatch? = null
        private set

    constructor(classLoader: ClassLoader, stopWatch: StopWatch) : this(classLoader) {
        this.stopWatch = stopWatch
    }

    override fun toString() = "UtContext(classLoader=$classLoader, hashCode=${hashCode()})"

    private class Cookie(context: UtContext) : AutoCloseable {
        private val contextToRestoreOnClose: UtContext? = threadLocalContextHolder.get()
        private val currentContext: UtContext = context

        init {
            threadLocalContextHolder.set(currentContext)
        }

        override fun close() {
            val context = threadLocalContextHolder.get()

            require(context === currentContext) {
                "Trying to close UtContext.Cookie but it seems that last set context $context is not equal context set on cookie creation $currentContext"
            }

            restore(contextToRestoreOnClose)
        }
    }


    companion object {
        private val Key = object : CoroutineContext.Key<UtContext> {}
        private val threadLocalContextHolder = ThreadLocal<UtContext>()

        fun currentContext(): UtContext? = threadLocalContextHolder.get()
        fun setUtContext(context: UtContext): AutoCloseable = Cookie(context)

        private fun restore(contextToRestore : UtContext?) {
            if (contextToRestore != null) {
                threadLocalContextHolder.set(contextToRestore)
            } else {
                threadLocalContextHolder.remove()
            }
        }
    }

    override val key: CoroutineContext.Key<UtContext> get() = Key

    override fun restoreThreadContext(context: CoroutineContext, oldState: UtContext?) = restore(oldState)

    override fun updateThreadContext(context: CoroutineContext): UtContext? {
        val prevUtContext = threadLocalContextHolder.get()
        threadLocalContextHolder.set(this)
        return prevUtContext
    }
}

inline fun <T> withUtContext(context: UtContext, block: () -> T): T = setUtContext(context).use {
    try {
        return@use block.invoke()
    } catch (e: Exception) {
        KotlinLogging.logger("withUtContext").error { e }
        throw e
    }
}
