package org.utbot.instrumentation.instrumentation

import org.utbot.instrumentation.util.Command
import java.lang.instrument.ClassFileTransformer

/**
 * Abstract class for the instrumentation.
 *
 * Except these two methods, should implement [transform] function which will be used to class instrumentation.
 *
 * @param TInvocationInstrumentation the return type of `invoke` function.
 */
interface Instrumentation<out TInvocationInstrumentation> : ClassFileTransformer {
    /**
     * Invokes a method with the given [methodSignature], the declaring class of which is [clazz], with the supplied
     * [arguments] and [parameters]. Parameters are additional data, the type of which depends on the specific implementation.
     *
     * @return Result of the invocation according to the specific implementation.
     */
    fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any? = null
    ): TInvocationInstrumentation

    /**
     * This function will be called from the child process loop every time it receives [Protocol.InstrumentationCommand] from the main process.
     *
     * @return Handles [cmd] and returns command which should be sent back to the [org.utbot.instrumentation.ConcreteExecutor].
     * If returns `null`, nothing will be sent.
     */
    fun <T : Command.InstrumentationCommand> handle(cmd: T): Command? {
        return null
    }

    /**
     * Will be called in the very beginning in the child process.
     */
    fun init(pathsToUserClasses: Set<String>) {}
}