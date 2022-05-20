package org.utbot.framework.util

val Throwable.description
    get() = message?.replace('\n', '\t') ?: "<Throwable with empty message>"