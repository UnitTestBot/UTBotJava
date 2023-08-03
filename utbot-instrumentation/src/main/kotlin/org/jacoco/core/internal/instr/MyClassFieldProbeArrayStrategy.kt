package org.jacoco.core.internal.instr

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.utbot.instrumentation.Settings

/**
 * Inspired by [org.jacoco.core.internal.instr.ClassFieldProbeArrayStrategy]
 */
class MyClassFieldProbeArrayStrategy(private val className: String) : IProbeArrayStrategy {

    private val FRAME_STACK_EMPTY = arrayOf<Any>(0)

    private val FRAME_LOCALS_EMPTY = arrayOfNulls<Any>(0)

    override fun storeInstance(mv: MethodVisitor, clinit: Boolean, variable: Int): Int {
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            className,
            // TODO change method name
            InstrSupport.INITMETHOD_NAME,
            InstrSupport.INITMETHOD_DESC,
            false
        )
        mv.visitVarInsn(Opcodes.ASTORE, variable)
        return 1
    }

    override fun addMembers(cv: ClassVisitor, probeCount: Int) {
        createDataField(cv)
        createInitMethod(cv, probeCount)
    }

    private fun createDataField(cv: ClassVisitor) {
        cv.visitField(
            Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_SYNTHETIC,
            Settings.PROBES_ARRAY_NAME,
            Settings.PROBES_ARRAY_DESC,
            null,
            null
        )
    }

    private fun createInitMethod(cv: ClassVisitor, probeCount: Int) {
        val mv = cv.visitMethod(
            InstrSupport.INITMETHOD_ACC,
            // TODO change method name
            InstrSupport.INITMETHOD_NAME,
            InstrSupport.INITMETHOD_DESC,
            null,
            null
        )

        mv.visitCode()
        mv.visitFieldInsn(
            Opcodes.GETSTATIC,
            className,
            Settings.PROBES_ARRAY_NAME,
            Settings.PROBES_ARRAY_DESC
        )
        val alreadyInitialized = Label()
        mv.visitJumpInsn(Opcodes.IFNONNULL, alreadyInitialized)
        mv.visitIntInsn(Opcodes.BIPUSH, probeCount)
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN)
        mv.visitFieldInsn(
            Opcodes.PUTSTATIC,
            className,
            Settings.PROBES_ARRAY_NAME,
            Settings.PROBES_ARRAY_DESC
        )
        mv.visitFrame(
            Opcodes.F_NEW,
            0,
            FRAME_LOCALS_EMPTY,
            0,
            FRAME_STACK_EMPTY
        )
        mv.visitLabel(alreadyInitialized)
        mv.visitFieldInsn(
            Opcodes.GETSTATIC,
            className,
            Settings.PROBES_ARRAY_NAME,
            Settings.PROBES_ARRAY_DESC
        )
        mv.visitInsn(Opcodes.ARETURN)

        mv.visitMaxs(1, 0)
        mv.visitEnd()
    }

}