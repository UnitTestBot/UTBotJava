package org.utbot.framework.plugin.api.visible

/**
 * An artificial exception that stores an exception that would be thrown in case of consuming stream by invoking terminal operations.
 * [innerException] stores this possible exception (null if [UtStreamConsumingException] was constructed by the engine).
 *
 * NOTE: this class should be visible in almost all parts of the tool - Soot, engine, concrete execution and code generation,
 * that's the reason why this class is placed in this module and this package is called `visible`.
 */
data class UtStreamConsumingException(private val innerException: Exception?) : RuntimeException() {
    /**
     * Returns the original exception [innerException] if possible, and any [RuntimeException] otherwise.
     */
    val innerExceptionOrAny: Throwable
        get() = innerException ?: RuntimeException("Unknown runtime exception during consuming stream")

    override fun toString(): String = innerExceptionOrAny.toString()
}