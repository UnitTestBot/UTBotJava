package org.utbot.instrumentation.instrumentation.et

import org.jacoco.core.internal.instr.createClassVisitorForTracingBranchInstructions
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import java.security.ProtectionDomain

class ExecutionBranchTraceInstrumentation : ExecutionTraceInstrumentation() {

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray {
        val instrumenter = Instrumenter(classfileBuffer)

        traceHandler.registerClass(className)
        instrumenter.visitClass { writer ->
            createClassVisitorForTracingBranchInstructions(className, traceHandler.processingStorage, writer)
        }

        return instrumenter.classByteCode
    }

    object Factory : Instrumentation.Factory<Trace, ExecutionBranchTraceInstrumentation> {
        override fun create(): ExecutionBranchTraceInstrumentation = ExecutionBranchTraceInstrumentation()
    }
}