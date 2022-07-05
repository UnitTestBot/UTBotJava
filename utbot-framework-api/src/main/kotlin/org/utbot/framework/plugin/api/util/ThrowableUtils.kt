package org.utbot.framework.plugin.api.util

import org.utbot.framework.plugin.api.ClassId

val Throwable.description
    get() = message?.replace('\n', '\t') ?: "<Throwable with empty message>"

val Throwable.isCheckedException
    get() = !(this is RuntimeException|| this is Error)

val ClassId.isCheckedException
    get() = !(RuntimeException::class.java.isAssignableFrom(this.jClass) || Error::class.java.isAssignableFrom(this.jClass))