@file:UseSerializers(UtContextThrowableSerializer::class)
package org.utbot.framework.plugin.api

import kotlinx.serialization.UseSerializers
import org.utbot.framework.UtContextThrowableSerializer
import kotlinx.serialization.Serializable
import java.io.File
import java.util.LinkedList

@Serializable
sealed class UtExecutionResult

@Serializable
data class UtExecutionSuccess(val model: UtModel) : UtExecutionResult() {
    override fun toString() = "$model"
}

@Serializable
sealed class UtExecutionFailure : UtExecutionResult() {
    abstract val exception: Throwable
    val isCheckedException get() = !(exception is RuntimeException || exception is Error)
}

@Serializable
data class UtOverflowFailure(
    override val exception: Throwable,
) : UtExecutionFailure()

/**
 * unexpectedFail (when exceptions such as NPE, IOBE, etc. appear, but not thrown by a user, applies both for function under test and nested calls )
 * expectedCheckedThrow (when function under test or nested call explicitly says that checked exception could be thrown and throws it)
 * expectedUncheckedThrow (when there is a throw statement for unchecked exception inside of function under test)
 * unexpectedUncheckedThrow (in case when there is unchecked exception thrown from nested call)
 */
@Serializable
data class UtExplicitlyThrownException(
    override val exception: Throwable,
    val fromNestedMethod: Boolean
) : UtExecutionFailure()

@Serializable
data class UtImplicitlyThrownException(
    override val exception: Throwable,
    val fromNestedMethod: Boolean
) : UtExecutionFailure()

@Serializable
class TimeoutException : Exception {
    constructor(s: String) : super(s)
}

@Serializable
data class UtTimeoutException(override val exception: TimeoutException) : UtExecutionFailure()

/**
 * Indicates failure in concrete execution.
 * For now it is explicitly throwing by ConcreteExecutor in case child process death.
 */
@Serializable
class ConcreteExecutionFailureException : Exception {
    val processStdout: List<String>

    constructor(cause: Throwable, errorFile: File, processStdoutArgument: List<String>) :
            super(
                buildString
                {
                    appendLine()
                    appendLine("----------------------------------------")
                    appendLine("The child process is dead")
                    appendLine("Cause:\n${cause.message}")
                    appendLine("Last 20 lines of the error log ${errorFile.absolutePath}:")
                    appendLine("----------------------------------------")
                    errorFile.useLines { lines ->
                        val lastLines = LinkedList<String>()
                        for (line in lines) {
                            lastLines.add(line)
                            if (lastLines.size > 20) {
                                lastLines.removeFirst()
                            }
                        }
                        lastLines.forEach { appendLine(it) }
                    }
                    appendLine("----------------------------------------")
                },
                cause
            ) {
        processStdout = processStdoutArgument
    }
}

@Serializable
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
    if (this is UtExecutionFailure) action(exception)
    return this
}

fun UtExecutionResult.getOrThrow(): UtModel = when (this) {
    is UtExecutionSuccess -> model
    is UtExecutionFailure -> throw exception
}

fun UtExecutionResult.exceptionOrNull(): Throwable? = when (this) {
    is UtExecutionFailure -> exception
    is UtExecutionSuccess -> null
}
