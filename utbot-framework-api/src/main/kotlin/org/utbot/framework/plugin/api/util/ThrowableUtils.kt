package org.utbot.framework.plugin.api.util

val Throwable.description
    get() = message?.replace('\n', '\t') ?: "<Throwable with empty message>"

val Throwable.isCheckedException
    get() = !(this is RuntimeException || this is Error)

val Class<*>.isCheckedException
    get() = !(RuntimeException::class.java.isAssignableFrom(this) || Error::class.java.isAssignableFrom(this))