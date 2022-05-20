package org.utbot.intellij.plugin.settings

import org.utbot.framework.plugin.api.UtService

abstract class UtServiceLoader<T> {
    abstract val services: List<UtService<T>>
    abstract val defaultService: UtService<T>
    protected val serviceByName: MutableMap<String, UtService<T>> = mutableMapOf()

    val defaultServiceProvider: T
        get() = defaultService.serviceProvider

    val defaultServiceProviderName: String
        get() = defaultService.displayName

    fun serviceProviderByName(name: String): T? =
        serviceByName[name]?.serviceProvider

    val serviceProviderNames: Set<String>
        get() = serviceByName.keys

    protected inline fun <T> withLocalClassLoader(block: () -> T): T {
        val actualClassLoader = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = this::class.java.classLoader
            return block()
        } finally {
            Thread.currentThread().contextClassLoader = actualClassLoader
        }
    }
}