package org.utbot.instrumentation.util

import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation

/**
 * This object represents base commands for interprocess communication.
 */
object Protocol {
    /**
     * Abstract class for all commands.
     */
    abstract class Command

    /**
     * The child process sends this command to the main process when operation completed.
     */
    class OperationCompleted : Command()

    /**
     * The main process tells where the child process should search for the classes.
     */
    data class AddPathsCommand(
        val pathsToUserClasses: String,
        val pathsToDependencyClasses: String
    ) : Command()

    /**
     * The main process sends [instrumentation] to the child process.
     */
    data class SetInstrumentationCommand<TIResult>(
        val instrumentation: Instrumentation<TIResult>
    ) : Command()

    /**
     * The main process requests the child process to execute a method with the given [signature],
     * which declaring class's name is [className].
     *
     * @property parameters are the parameters needed for an execution, e.g. static environment.
     */
    data class InvokeMethodCommand(
        val className: String,
        val signature: String,
        val arguments: ArgumentList,
        val parameters: Any?,
    ) : Command()

    /**
     * The child process returns the result of the invocation to the main process.
     */
    data class InvocationResultCommand<T>(
        val result: T
    ) : Command()

    /**
     * Warmup - load classes from classpath and instrument them
     */
    class WarmupCommand() : Command()

    /**
     * The child process sends this command if unexpected exception was thrown.
     *
     * @property exception unexpected exception.
     */
    data class ExceptionInChildProcess(
        val exception: Throwable
    ) : Command()

    /**
     * This command tells the child process to stop.
     */
    class StopProcessCommand : Command()

    /**
     * [org.utbot.instrumentation.ConcreteExecutor] can send other commands depending on specific instrumentation.
     * This commands will be handled in [Instrumentation.handle] function.
     *
     * Only inheritors of this abstract class will be passed in [Instrumentation.handle] function.
     */
    abstract class InstrumentationCommand : Protocol.Command()
}


