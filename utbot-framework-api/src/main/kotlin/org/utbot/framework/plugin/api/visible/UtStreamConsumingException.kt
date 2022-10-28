package org.utbot.framework.plugin.api.visible

/**
 * An artificial exception that stores an exception that would be thrown in case of consuming stream by invoking terminal operations.
 * [innerException] stores this possible exception (null if [UtStreamConsumingException] was constructed by the engine).
 */
data class UtStreamConsumingException(private val innerException: Exception?) : RuntimeException() {
    /**
     * Returns the original exception [innerException] if possible, and any [RuntimeException] otherwise.
     */
    val innerExceptionOrAny: Throwable
        get() = innerException ?: RuntimeException("Unknown runtime exception during consuming stream")

    override fun toString(): String = innerExceptionOrAny.toString()
}