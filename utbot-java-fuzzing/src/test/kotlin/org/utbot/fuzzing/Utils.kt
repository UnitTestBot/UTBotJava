package org.utbot.fuzzing

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext

internal fun <T, V> V.runBlockingWithContext(block: suspend () -> T) : T {
    return withUtContext(UtContext(this!!::class.java.classLoader)) {
        runBlocking {
            withTimeout(10000) {
                block()
            }
        }
    }
}