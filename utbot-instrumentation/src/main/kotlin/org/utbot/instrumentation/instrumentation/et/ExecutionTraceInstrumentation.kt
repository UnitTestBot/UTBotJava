package org.utbot.instrumentation.instrumentation.et

import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.InvokeWithStaticsInstrumentation
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import java.security.ProtectionDomain
import org.utbot.framework.plugin.api.FieldId

/**
 * This instrumentation allows to get execution trace during each call.
 */

class ExecutionTraceInstrumentation : Instrumentation<Trace> {
    private val invokeWithStatics = InvokeWithStaticsInstrumentation()
    private val traceHandler = TraceHandler()

    /**
     * Invokes a method with the given [methodSignature], the declaring class of which is [clazz], with the supplied
     * [arguments] and [parameters].
     *
     * @return Trace of this invocation.
     */
    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?
    ): Trace {
        invokeWithStatics.invoke(clazz, methodSignature, arguments, parameters)

        val trace = traceHandler.computeTrace()
        traceHandler.resetTrace()

        return trace
    }

    override fun getStaticField(fieldId: FieldId): Result<*> =
        invokeWithStatics.getStaticField(fieldId)

    /**
     * Transforms bytecode such way that it becomes possible to get an execution trace during a call.
     *
     * Adds set of instructions which write down the executed instruction and method call id consistently.
     */
    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray {
        traceHandler.registerClass(className)
        return Instrumenter(classfileBuffer).run {
            val visitor = traceHandler.computeInstructionVisitor(className)
            visitInstructions(visitor)
            classByteCode
        }
    }

    class Factory : Instrumentation.Factory<Trace, ExecutionTraceInstrumentation> {
        override fun create(): ExecutionTraceInstrumentation = ExecutionTraceInstrumentation()
    }
}