package org.utbot.instrumentation.instrumentation.instrumenter.visitors.util

import org.utbot.instrumentation.Settings
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

// TODO: document this

class AddFieldAdapter(
    classVisitor: ClassVisitor,
    private val instanceFieldInitializer: InstanceFieldInitializer
) : ClassVisitor(Settings.ASM_API, classVisitor) {
    private var isFieldPresent = false
    private var isZeroArgConstructorPresent = false

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?
    ): FieldVisitor {
        if (name == instanceFieldInitializer.name) {
            isFieldPresent = true
        }
        return cv.visitField(access, name, descriptor, signature, value)
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        var v = cv.visitMethod(access, name, descriptor, signature, exceptions)
        if (name == "<init>" && descriptor == "()V") {
            isZeroArgConstructorPresent = true
            v = InitFieldMethodAdapter(v, instanceFieldInitializer)
        }
        return v
    }

    override fun visitEnd() {
        if (!isFieldPresent) {
            val fv = cv.visitField(
                Opcodes.ACC_PUBLIC + Opcodes.ACC_SYNTHETIC,
                instanceFieldInitializer.name,
                instanceFieldInitializer.descriptor,
                instanceFieldInitializer.signature,
                null
            )
            if (fv != null) {
                fv.visitEnd()
            }
            cv.visitEnd()
        } else {
            throw Exception("Field ${instanceFieldInitializer.name} already exists")
        }

        if (!isZeroArgConstructorPresent) {
            val mv = InitFieldMethodAdapter(
                cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null),
                instanceFieldInitializer
            )
            mv.visitCode()
            mv.visitVarInsn(Opcodes.ALOAD, 0)
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            mv.visitInsn(Opcodes.RETURN)
            mv.visitMaxs(1, 1)
            mv.visitEnd()
        }

        super.visitEnd()
    }

    // TODO: document this

    class InitFieldMethodAdapter(
        methodVisitor: MethodVisitor,
        private val instanceFieldInitializer: InstanceFieldInitializer
    ) : MethodVisitor(Settings.ASM_API, methodVisitor) {
        override fun visitCode() {
            mv.visitCode()
            instanceFieldInitializer.initField(mv)
        }
    }
}