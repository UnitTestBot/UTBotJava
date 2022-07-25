package org.utbot.instrumentation.classloaders

import org.utbot.instrumentation.instrumentation.Instrumentation

/**
 * Old ClassLoader for loading user's classes and instrumenting them.
 *
 * It's combination of [UserRuntimeClassLoader] and [InstrumentationClassLoader].
 *
 * Note: It's used with value-based [Instrumentation]s because there isn't a common way to exchange any objects between ClassLoaders.
 */
internal class HandlerClassLoader(urls: Iterable<String>) : UserRuntimeClassLoader(urls, getSystemClassLoader()) {

    /**
     * HACK. System classloader can find org.slf4j thus we want that this class will be loaded by [HandlerClassLoader]
     * when we want to mock something from org.slf4j.
     */
    override val packsToAlwaysInstrument: List<String>
        get() = listOf("org.slf4j") + super.packsToAlwaysInstrument

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (packsToAlwaysInstrument.any(name::startsWith)) {
            return (findLoadedClass(name) ?: findClass(name)).apply {
                if (resolve) resolveClass(this)
            }
        }
        return super.loadClass(name, resolve)
    }
}
