package org.utbot.contest

import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import org.utbot.framework.plugin.api.util.UtContext

object ContextManager {
    private val currentContexts = mutableListOf<UtContext>()

    fun createNewContext(classLoader: ClassLoader) =
        UtContext(classLoader).apply {
            currentContexts.add(this)
        }

    fun cancelAll() {
        currentContexts.forEach {
            it.cancelChildren()
            it.cancel()
        }
        currentContexts.clear()
    }

}