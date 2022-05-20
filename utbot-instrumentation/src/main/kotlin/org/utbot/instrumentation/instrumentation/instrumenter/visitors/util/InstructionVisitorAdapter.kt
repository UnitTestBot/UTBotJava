package org.utbot.instrumentation.instrumentation.instrumenter.visitors.util

import org.utbot.instrumentation.Settings
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.LocalVariablesSorter

// TODO: document this

class InstructionVisitorAdapter(
    classVisitor: ClassVisitor,
    val methodName: String?,
    val instructionVisitor: IInstructionVisitor
) : ClassVisitor(Settings.ASM_API, classVisitor) {
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val v = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (name != "<clinit>" && (methodName == null || name == methodName)) {
            instructionVisitor.visitMethod(access, name, descriptor, signature, exceptions, v)
            val iv = InstructionVisitorMethodAdapter(v, instructionVisitor, name)
            iv.lvs = LocalVariablesSorter(access, descriptor, iv)
            return iv.lvs!!
        }
        return v
    }
}

