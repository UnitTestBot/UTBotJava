package org.utbot.framework.concrete.phases

abstract class PhaseError(message: String, cause: Throwable) : Exception(message, cause) {
    override val cause: Throwable = super.cause!!
}

interface PhaseContext<E: PhaseError> {
    fun wrapError(error: Throwable): E
}

inline fun <reified R, T: PhaseContext<*>> T.start(block: T.() -> R): R =
    try {
        block()
    } catch (e: Throwable) {
        throw wrapError(e)
    }
