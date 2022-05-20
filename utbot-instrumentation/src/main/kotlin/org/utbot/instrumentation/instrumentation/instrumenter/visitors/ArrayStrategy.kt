package org.utbot.instrumentation.instrumentation.instrumenter.visitors

import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.instrumenter.visitors.util.IInstructionVisitor
import org.utbot.instrumentation.instrumentation.instrumenter.visitors.util.InstanceFieldInitializer
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

// TODO: document this

class ArrayStrategy(
    private val className: String,
    override val name: String
) : InstanceFieldInitializer, IInstructionVisitor {
    override val signature: String? = null
    override val descriptor: String = Settings.PROBES_ARRAY_DESC

    override fun initField(mv: MethodVisitor): MethodVisitor {
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitLdcInsn(probesCount)
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN)
        mv.visitFieldInsn(Opcodes.PUTFIELD, className, name, Settings.PROBES_ARRAY_DESC)
        return mv
    }

    override fun visitInstruction(mv: MethodVisitor): MethodVisitor {
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitFieldInsn(Opcodes.GETFIELD, className, name, Settings.PROBES_ARRAY_DESC)
        mv.visitLdcInsn(nextProbeId())
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.BASTORE)
        return mv
    }

    private var probeId = 0

    private fun nextProbeId(): Int {
        return probeId++
    }

    val probesCount: Int
        get() = probeId
}