package org.jacoco.core.internal.instr

import org.jacoco.core.internal.flow.MethodProbesVisitor
import org.objectweb.asm.ClassVisitor

/**
 * Just a copy of [ClassInstrumenter] with one overridden method.
 * This class is used to replace probe inserter.
 */
class NoneClassInstrumenter(
    probeArrayStrategy: IProbeArrayStrategy,
    cv: ClassVisitor,
    private val methodToProbes: MutableMap<String, MutableList<Int>>,
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
        val probeVariableInserter = NoneProbeInserter(name, desc, methodToProbes, frameEliminator)
        return MethodInstrumenter(probeVariableInserter, probeVariableInserter)
    }

}