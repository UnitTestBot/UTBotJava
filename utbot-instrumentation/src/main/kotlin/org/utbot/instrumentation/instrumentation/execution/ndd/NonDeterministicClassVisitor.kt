package org.utbot.instrumentation.instrumentation.execution.ndd

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.utbot.instrumentation.Settings

class NonDeterministicClassVisitor(
    classVisitor: ClassVisitor,
    private val detector: NonDeterministicDetector
) : ClassVisitor(Settings.ASM_API, classVisitor) {

    private lateinit var currentClass: String

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        currentClass = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = cv.visitMethod(access, name, descriptor, signature, exceptions)
        return object : MethodVisitor(Settings.ASM_API, mv) {
            override fun visitMethodInsn(
                opcodeAndSource: Int,
                owner: String,
                name: String,
                descriptor: String,
                isInterface: Boolean
            ) {
                if (name == "<init>") {
                    mv.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
                    if (detector.isNonDeterministicClass(owner)) {
                        detector.inserter.insertAfterNDInstanceConstructor(mv, currentClass)
                    }
                    return
                }

                val (isND, isStatic) = if (opcodeAndSource == Opcodes.INVOKESTATIC) {
                    detector.isNonDeterministicStaticFunction(owner, name, descriptor) to true
                } else {
                    detector.isNonDeterministicClass(owner) to false
                }

                if (isND) {
                    detector.inserter.insertBeforeNDMethod(mv, descriptor, isStatic)
                    mv.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
                    detector.inserter.insertAfterNDMethod(mv, owner, name, descriptor, isStatic)
                } else {
                    mv.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
                }

            }
        }
    }
}