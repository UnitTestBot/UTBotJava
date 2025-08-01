package org.utbot.instrumentation.instrumentation.instrumenter.visitors.util

import org.utbot.instrumentation.Settings
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.LocalVariablesSorter

// TODO: document this
open class InstructionVisitorMethodAdapter(
    mv: MethodVisitor,
    private val instructionVisitor: IInstructionVisitor,
    private val methodName: String
) : MethodVisitor(Settings.ASM_API, mv) {
    var lvs: LocalVariablesSorter? = null

    companion object {
        val returnInsns = setOf(
            Opcodes.IRETURN,
            Opcodes.LRETURN,
            Opcodes.FRETURN,
            Opcodes.DRETURN,
            Opcodes.ARETURN,
            Opcodes.RETURN
        )
    }


    override fun visitLineNumber(line: Int, start: Label?) {
        instructionVisitor.visitLine(mv, line)
        super.visitLineNumber(line, start)
    }

    override fun visitCode() {
        mv.visitCode()
        instructionVisitor.visitCode(mv, lvs!!)
    }

    override fun visitInsn(opcode: Int) {
        when (opcode) {
            in returnInsns -> instructionVisitor.visitReturnInstruction(mv, opcode)
            Opcodes.ATHROW -> instructionVisitor.visitThrowInstruction(mv)
            else -> instructionVisitor.visitInstruction(mv)
        }
        super.visitInsn(opcode)
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        instructionVisitor.visitInstruction(mv)
        super.visitIntInsn(opcode, operand)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        instructionVisitor.visitInstruction(mv)
        super.visitVarInsn(opcode, `var`)
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        instructionVisitor.visitInstruction(mv)
        super.visitTypeInsn(opcode, type)
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        instructionVisitor.visitFieldInstruction(mv, opcode, owner, name, descriptor)
        super.visitFieldInsn(opcode, owner, name, descriptor)
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        instructionVisitor.visitMethodInstruction(mv, opcode, owner, name, descriptor, isInterface)
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ) {
        instructionVisitor.visitInstruction(mv)
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
    }

    override fun visitJumpInsn(opcode: Int, label: Label?) {
        instructionVisitor.visitInstruction(mv)
        super.visitJumpInsn(opcode, label)
    }

    override fun visitLdcInsn(value: Any?) {
        instructionVisitor.visitInstruction(mv)
        super.visitLdcInsn(value)
    }

    override fun visitIincInsn(`var`: Int, increment: Int) {
        instructionVisitor.visitInstruction(mv)
        super.visitIincInsn(`var`, increment)
    }

    override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label?, vararg labels: Label?) {
        instructionVisitor.visitInstruction(mv)
        super.visitTableSwitchInsn(min, max, dflt, *labels)
    }

    override fun visitLookupSwitchInsn(dflt: Label?, keys: IntArray?, labels: Array<out Label>?) {
        instructionVisitor.visitInstruction(mv)
        super.visitLookupSwitchInsn(dflt, keys, labels)
    }

    override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
        instructionVisitor.visitInstruction(mv)
        super.visitMultiANewArrayInsn(descriptor, numDimensions)
    }
}
