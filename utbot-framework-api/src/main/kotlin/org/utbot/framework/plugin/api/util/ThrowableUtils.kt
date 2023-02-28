package org.utbot.framework.plugin.api.util

import org.utbot.framework.plugin.api.OverflowDetectionError
import org.utbot.framework.plugin.api.TimeoutException

val Throwable.description
    get() = message?.replace('\n', '\t') ?: "<Throwable with empty message>"

val Throwable.isCheckedException
    get() = !(this is RuntimeException || this is Error)

val Throwable.prettyName
    get() = when (this) {
        is OverflowDetectionError -> "Overflow"
        is TimeoutException -> "Timeout"
        else -> this::class.simpleName
    }

val Class<*>.isCheckedException
    get() = !(RuntimeException::class.java.isAssignableFrom(this) || Error::class.java.isAssignableFrom(this))