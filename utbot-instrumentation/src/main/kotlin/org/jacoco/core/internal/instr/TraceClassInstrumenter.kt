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
    private val storage: ProcessingStorage,
    private val nextIdGenerator: (localId: Int) -> Long
) : ClassInstrumenter(probeArrayStrategy, cv) {

    override fun visitMethod(
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodProbesVisitor {
        val mv = cv.visitMethod(access, name, desc, signature, exceptions)

        val frameEliminator = DuplicateFrameEliminator(mv)
        val probeVariableInserter = TraceProbeInserter(
            access, name, desc, frameEliminator, probeArrayStrategy, nextIdGenerator
        )
        return TraceMethodInstrumenter(
            name, desc, probeVariableInserter, probeVariableInserter, storage, nextIdGenerator
        )
    }

}