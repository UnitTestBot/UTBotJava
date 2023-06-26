package org.utbot.common

import java.net.URLClassLoader

/**
 * Checks that the class given by its binary name can be loaded with given classLoader.
 */
fun URLClassLoader.hasOnClasspath(classBinaryName: String): Boolean {
    val classFqn = classBinaryName.replace('.', '/').plus(".class")
    return this.findResource(classFqn) != null
}