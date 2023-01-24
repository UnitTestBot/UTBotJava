package org.utbot.framework.plugin.api

import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.visible.UtStreamConsumingException
import java.io.File
import java.util.LinkedList

sealed class UtExecutionResult

data class UtExecutionSuccess(val model: UtModel) : UtExecutionResult() {
    override fun toString() = "$model"
}

sealed class UtExecutionFailure : UtExecutionResult() {
    abstract val exception: Throwable

    /**
     * Represents the most inner exception in the failure.
     * Often equals to [exception], but is wrapped exception in [UtStreamConsumingException].
     */
    open val rootCauseException: Throwable
        get() = exception
}

data class UtOverflowFailure(
    override val exception: Throwable,
) : UtExecutionFailure()

data class UtSandboxFailure(
    override val exception: Throwable
) : UtExecutionFailure()

data class UtStreamConsumingFailure(
    override val exception: UtStreamConsumingException,
) : UtExecutionFailure() {
    override val rootCauseException: Throwable
        get() = exception.innerExceptionOrAny
}

/**
 * unexpectedFail (when exceptions such as NPE, IOBE, etc. appear, but not thrown by a user, applies both for function under test and nested calls )
 * expectedCheckedThrow (when function under test or nested call explicitly says that checked exception could be thrown and throws it)
 * expectedUncheckedThrow (when there is a throw statement for unchecked exception inside of function under test)
 * unexpectedUncheckedThrow (in case when there is unchecked exception thrown from nested call)
 */
data class UtExplicitlyThrownException(
    override val exception: Throwable,
    val fromNestedMethod: Boolean
) : UtExecutionFailure()

data class UtImplicitlyThrownException(
    override val exception: Throwable,
    val fromNestedMethod: Boolean
) : UtExecutionFailure()

class TimeoutException(s: String) : Exception(s)

data class UtTimeoutException(override val exception: TimeoutException) : UtExecutionFailure()

/**
 * Indicates failure in concrete execution.
 * For now it is explicitly throwing by ConcreteExecutor in case instrumented process death.
 */
class ConcreteExecutionFailureException(cause: Throwable, errorFile: File, val processStdout: List<String>) :
    Exception(
        buildString {
            appendLine()
            appendLine("----------------------------------------")
            appendLine("The instrumented process is dead")
            appendLine("Cause:\n${cause.message}")
            appendLine("Last 1000 lines of the error log ${errorFile.absolutePath}:")
            appendLine("----------------------------------------")
            if (!errorFile.exists()) {
                errorFile.createNewFile()
            }
            errorFile.useLines { lines ->
                val lastLines = LinkedList<String>()
                for (line in lines) {
                    lastLines.add(line)
                    if (lastLines.size > 1000) {
                        lastLines.removeFirst()
                    }
                }
                lastLines.forEach { appendLine(it) }
            }
            appendLine("----------------------------------------")
        },
        cause
    )

data class UtConcreteExecutionFailure(override val exception: ConcreteExecutionFailureException) : UtExecutionFailure()

val UtExecutionResult.isSuccess: Boolean
    get() = this is UtExecutionSuccess

val UtExecutionResult.isFailure: Boolean
    get() = this is UtExecutionFailure

inline fun UtExecutionResult.onSuccess(action: (model: UtModel) -> Unit): UtExecutionResult {
    if (this is UtExecutionSuccess) action(model)
    return this
}

inline fun UtExecutionResult.onFailure(action: (exception: Throwable) -> Unit): UtExecutionResult {
    if (this is UtExecutionFailure) action(rootCauseException)
    return this
}

fun UtExecutionResult.getOrThrow(): UtModel = when (this) {
    is UtExecutionSuccess -> model
    is UtExecutionFailure -> throw exception
}

fun UtExecutionResult.exceptionOrNull(): Throwable? = when (this) {
    is UtExecutionFailure -> rootCauseException
    is UtExecutionSuccess -> null
}
