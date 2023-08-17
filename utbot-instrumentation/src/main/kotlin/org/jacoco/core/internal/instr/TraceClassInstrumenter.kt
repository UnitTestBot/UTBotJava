package org.jacoco.core.internal.instr

import org.jacoco.core.internal.flow.MethodProbesVisitor
import org.objectweb.asm.ClassVisitor
import org.utbot.instrumentation.instrumentation.et.ProcessingStorage

/**
 * Just a copy of [ClassInstrumenter] with one overridden method.
 * This class is used to replace probe inserter and method instrumenter.
 */
class TraceClassInstrumenter(
    private val probeArrayStrategy: IProbeArrayStrategy,
    cv: ClassVisitor,
    private val className: String,
    private val storage: ProcessingStorage,
    private val probeIdGenerator: ProbeIdGenerator
) : ClassInstrumenter(probeArrayStrategy, cv) {

    override fun visitMethod(
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodProbesVisitor {
        val mv = cv.visitMethod(access, name, desc, signature, exceptions)

        val currentMethodSignature = name + desc
        storage.addClassMethod(className, currentMethodSignature)

        val frameEliminator = DuplicateFrameEliminator(mv)
        val probeVariableInserter = TraceProbeInserter(
            access, name, desc, frameEliminator, probeArrayStrategy, probeIdGenerator
        )
        return TraceMethodInstrumenter(
            currentMethodSignature, probeVariableInserter, probeVariableInserter, storage, probeIdGenerator
        )
    }

}