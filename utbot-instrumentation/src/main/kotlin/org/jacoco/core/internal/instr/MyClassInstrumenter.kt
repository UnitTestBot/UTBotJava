package org.jacoco.core.internal.instr

import org.jacoco.core.internal.flow.MethodProbesVisitor
import org.objectweb.asm.ClassVisitor

class MyClassInstrumenter(
    probeArrayStrategy: IProbeArrayStrategy,
    cv: ClassVisitor
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
        val probeVariableInserter = MyProbeInserter(name, frameEliminator)
        return MyMethodInstrumenter(probeVariableInserter, probeVariableInserter)
    }

}