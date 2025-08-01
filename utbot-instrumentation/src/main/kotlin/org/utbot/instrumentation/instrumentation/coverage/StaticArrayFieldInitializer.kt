package org.utbot.instrumentation.instrumentation.coverage

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.instrumenter.visitors.util.StaticFieldInitializer

class StaticArrayFieldInitializer(
    private val className: String,
    override val name: String,
    private val probesCount: Int
) : StaticFieldInitializer {

    override val descriptor: String = Settings.PROBES_ARRAY_DESC
    override val signature: String? = null

    override fun initField(mv: MethodVisitor): MethodVisitor {
        mv.visitLdcInsn(probesCount)
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN)
        mv.visitFieldInsn(Opcodes.PUTSTATIC, className, name, Settings.PROBES_ARRAY_DESC)
        return mv
    }

}