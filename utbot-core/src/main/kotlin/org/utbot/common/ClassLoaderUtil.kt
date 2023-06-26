package org.utbot.common

import java.net.URLClassLoader

/**
 * Checks that the class given by its binary name is on classpath of this classloader.
 *
 * Note: if the specified class is on classpath, `true` is returned even when
 * superclass (or implemented interfaces) aren't on the classpath.
 */
fun URLClassLoader.hasOnClasspath(classBinaryName: String): Boolean {
    val classFqn = classBinaryName.replace('.', '/').plus(".class")
    return this.findResource(classFqn) != null
}