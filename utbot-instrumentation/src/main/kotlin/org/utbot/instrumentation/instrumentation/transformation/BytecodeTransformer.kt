package org.utbot.instrumentation.instrumentation.transformation

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.transformation.adapters.StringEqualsMethodAdapter

/**
 * Main class for the transformation.
 * Bytecode transformations will be combined in this class.
 */
class BytecodeTransformer(classVisitor: ClassVisitor) : ClassVisitor(Settings.ASM_API, classVisitor) {
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val methodVisitor = cv.visitMethod(access, name, descriptor, signature, exceptions)
        return StringEqualsMethodAdapter(api, methodVisitor)
    }
}