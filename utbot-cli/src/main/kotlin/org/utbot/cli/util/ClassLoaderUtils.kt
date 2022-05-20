package org.utbot.cli.util

import java.io.File
import java.net.URL
import java.net.URLClassLoader

private fun String.toUrl(): URL = File(this).toURI().toURL()

fun createClassLoader(classPath: String? = "", absoluteFileNameWithClasses: String? = null): URLClassLoader {
    val urlSet = mutableSetOf<URL>()
    classPath?.run {
        urlSet.addAll(this.split(File.pathSeparatorChar).map { it.toUrl() }.toMutableSet())
    }
    absoluteFileNameWithClasses?.run {
        urlSet.addAll(File(absoluteFileNameWithClasses).readLines().map { it.toUrl() }.toMutableSet())
    }
    val urls = urlSet.toTypedArray()
    return URLClassLoader(urls)
}