package org.utbot.engine.greyboxfuzzer.util

import java.net.URLClassLoader

object CustomClassLoader {
    lateinit var classLoader: ClassLoader

    fun isClassLoaderInitialized() = this::classLoader.isInitialized
}