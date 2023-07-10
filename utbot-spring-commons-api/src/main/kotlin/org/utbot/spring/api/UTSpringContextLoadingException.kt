package org.utbot.spring.api

/**
 * Used primarily to let code generation distinguish between
 * parts of stack trace inside UTBot (including RD, etc.)
 * and parts of stack trace inside Spring and user application.
 */
class UTSpringContextLoadingException(override val cause: Throwable) : Exception(
    "UTBot failed to load Spring application context",
    cause
)
