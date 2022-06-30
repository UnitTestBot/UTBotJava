package org.utbot.instrumentation.classloaders

import java.io.File
import java.net.URL
import java.net.URLClassLoader
import org.utbot.instrumentation.instrumentation.Instrumentation

/**
 * ClassLoader for invocation user's functions.
 *
 * Use this ClassLoader to instrument classes and invoke instrumented methods.
 */
open class UserRuntimeClassLoader(
    urls: Iterable<String>,
    parent: ClassLoader? = null
) : URLClassLoader(urls.toURLs(), parent) {
    open val packsToAlwaysInstrument: List<String> = listOf(
        "org.utbot.instrumentation.warmup"
    )
}

/**
 * ClassLoader for loading user's [Instrumentation]s.
 *
 * Classes loaded by this ClassLoader are not instrumented.
 */
class InstrumentationClassLoader(urls: Iterable<String>) : URLClassLoader(urls.toURLs())

fun Iterable<String>.toURLs(): Array<URL> =
    map { File(it).toURI().toURL() }.toTypedArray()
