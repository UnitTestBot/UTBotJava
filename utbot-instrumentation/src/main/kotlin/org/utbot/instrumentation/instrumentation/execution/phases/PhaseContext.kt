package org.utbot.instrumentation.instrumentation.execution.phases

abstract class PhaseError(message: String, override val cause: Throwable) : Exception(message)
interface PhaseContext<E: PhaseError> {
    fun wrapError(error: Throwable): E
}

internal inline fun <reified R, T: PhaseContext<*>> T.start(block: T.() -> R): R =
    try {
        block()
    } catch (e: UtConcreteExecutionForceException) {
        throw UtConcreteExecutionForceException(wrapError(e.cause))
    } catch (e: Throwable) {
        throw wrapError(e)
    }
