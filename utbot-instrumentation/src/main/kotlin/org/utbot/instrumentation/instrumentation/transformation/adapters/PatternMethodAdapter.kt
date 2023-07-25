package org.utbot.instrumentation.instrumentation.transformation.adapters

import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor


/**
 * Abstract class for the pattern method adapters.
 */
abstract class PatternMethodAdapter(api: Int, methodVisitor: MethodVisitor) : MethodVisitor(api, methodVisitor) {
    protected abstract fun resetState()

    override fun visitFrame(type: Int, numLocal: Int, local: Array<out Any>?, numStack: Int, stack: Array<out Any>?) {
        resetState()
        mv.visitFrame(type, numLocal, local, numStack, stack)
    }

    override fun visitInsn(opcode: Int) {
        resetState()
        mv.visitInsn(opcode)
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        resetState()
        mv.visitIntInsn(opcode, operand)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        resetState()
        mv.visitVarInsn(opcode, `var`)
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        resetState()
        mv.visitTypeInsn(opcode, type)
    }

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        resetState()
        mv.visitFieldInsn(opcode, owner, name, descriptor)
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        resetState()
        mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ) {
        resetState()
        mv.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
    }

    override fun visitJumpInsn(opcode: Int, label: Label?) {
        resetState()
        mv.visitJumpInsn(opcode, label)
    }

    override fun visitLabel(label: Label?) {
        resetState()
        mv.visitLabel(label)
    }

    override fun visitLdcInsn(value: Any?) {
        resetState()
        mv.visitLdcInsn(value)
    }

    override fun visitIincInsn(`var`: Int, increment: Int) {
        resetState()
        mv.visitIincInsn(`var`, increment)
    }

    override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label?, vararg labels: Label?) {
        resetState()
        mv.visitTableSwitchInsn(min, max, dflt, *labels)
    }

    override fun visitLookupSwitchInsn(dflt: Label?, keys: IntArray?, labels: Array<out Label>?) {
        resetState()
        mv.visitLookupSwitchInsn(dflt, keys, labels)
    }

    override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
        resetState()
        mv.visitMultiANewArrayInsn(descriptor, numDimensions)
    }

    override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
        resetState()
        mv.visitTryCatchBlock(start, end, handler, type)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        resetState()
        mv.visitMaxs(maxStack, maxLocals)
    }
}