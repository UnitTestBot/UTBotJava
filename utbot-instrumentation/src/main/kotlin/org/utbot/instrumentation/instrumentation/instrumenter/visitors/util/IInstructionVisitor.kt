package org.utbot.instrumentation.instrumentation.instrumenter.visitors.util

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.LocalVariablesSorter

// TODO: refactor this

// TODO: document this

interface IInstructionVisitor {
    fun visitLine(mv: MethodVisitor, line: Int): MethodVisitor {
        return mv
    }

    fun visitInstruction(mv: MethodVisitor): MethodVisitor {
        return mv
    }

    fun visitReturnInstruction(mv: MethodVisitor, opcode: Int): MethodVisitor {
        return visitInstruction(mv)
    }

    fun visitThrowInstruction(mv: MethodVisitor): MethodVisitor {
        return visitInstruction(mv)
    }


    fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?,
        methodVisitor: MethodVisitor
    ) {
    }

    fun visitMethodInstruction(
        mv: MethodVisitor,
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ): MethodVisitor {
        return visitInstruction(mv)
    }

    fun visitCode(mv: MethodVisitor, lvs: LocalVariablesSorter) {
    }

    fun visitFieldInstruction(
        mv: MethodVisitor,
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String
    ): MethodVisitor {
        return visitInstruction(mv)
    }
}