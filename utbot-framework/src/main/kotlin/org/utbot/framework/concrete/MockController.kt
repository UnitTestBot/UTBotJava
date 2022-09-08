package org.utbot.framework.concrete

import java.io.Closeable

interface MockController : Closeable {
    /**
     * Will be called before invocation on the same thread.
     */
    fun init()
}