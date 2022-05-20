package org.utbot.instrumentation.instrumentation.instrumenter.visitors.util

import org.utbot.instrumentation.Settings
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


// TODO: document this

class AddStaticFieldAdapter(
    classVisitor: ClassVisitor,
    private val staticFieldInitializer: StaticFieldInitializer
) : ClassVisitor(Settings.ASM_API, classVisitor) {
    private var isFieldPresent = false
    private var isClinitPresent = false
    private var isInterface = false

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        if (access and Opcodes.ACC_INTERFACE != 0) {
            isInterface = true
        }
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitField(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor {
        if (name == staticFieldInitializer.name) {
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
        if (name == "<clinit>" && descriptor == "()V") {
            isClinitPresent = true
            v = InitStaticFieldMethodAdapter(v, staticFieldInitializer)
        }
        return v
    }

    override fun visitEnd() {
        val finalOpcode = if (isInterface) Opcodes.ACC_FINAL else 0

        if (!isFieldPresent) {
            val fv = cv.visitField(
                Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_SYNTHETIC + finalOpcode,
                staticFieldInitializer.name,
                staticFieldInitializer.descriptor,
                staticFieldInitializer.signature,
                null
            )
            if (fv != null) {
                fv.visitEnd()
            }
            cv.visitEnd()
        } else {
            error("Field ${staticFieldInitializer.name} already exists")
        }

        if (!isClinitPresent) {
            val mv = InitStaticFieldMethodAdapter(
                cv.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null),
                staticFieldInitializer
            )
            mv.visitCode()
            mv.visitInsn(Opcodes.RETURN)
            mv.visitMaxs(1, 0)
            mv.visitEnd()
        }

        super.visitEnd()
    }

    // TODO: document this

    class InitStaticFieldMethodAdapter(
        methodVisitor: MethodVisitor,
        private val fieldInitializer: StaticFieldInitializer
    ) : MethodVisitor(Settings.ASM_API, methodVisitor) {
        override fun visitCode() {
            mv.visitCode()
            fieldInitializer.initField(mv)
        }
    }
}