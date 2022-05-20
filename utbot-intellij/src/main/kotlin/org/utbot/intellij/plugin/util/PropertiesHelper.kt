package org.utbot.intellij.plugin.util

import java.lang.IllegalArgumentException
import java.util.Properties

val props = object: Any() {}.javaClass.classLoader.getResourceAsStream("application.properties")
    .use { Properties().apply { load(it) } }

@Suppress("UNCHECKED_CAST")
inline fun <reified T> getProperty(key: String): T {
    val value = props.getProperty(key) ?: throw RuntimeException("could not find property $key")

    return when(T::class){
        Int::class -> value.toInt() as T
        String::class -> value as T
        else -> throw IllegalArgumentException("Argument of type [${T::class}] is not supported")
    }
}

