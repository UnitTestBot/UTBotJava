package org.jacoco.core.internal.instr

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.utbot.instrumentation.Settings

class MyClassFieldProbeArrayStrategy(private val className: String) : IProbeArrayStrategy {

    override fun storeInstance(mv: MethodVisitor, clinit: Boolean, variable: Int): Int {
        mv.visitFieldInsn(Opcodes.GETSTATIC, className, Settings.PROBES_ARRAY_NAME, Settings.PROBES_ARRAY_DESC)
        mv.visitVarInsn(Opcodes.ASTORE, variable)
        return 1
    }

    override fun addMembers(cv: ClassVisitor, probeCount: Int) {
    }

}