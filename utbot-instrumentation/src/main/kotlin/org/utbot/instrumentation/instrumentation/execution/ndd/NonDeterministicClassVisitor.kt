package org.utbot.instrumentation.instrumentation.execution.ndd

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.Method
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.instrumentation.Settings

class NonDeterministicClassVisitor(
    classVisitor: ClassVisitor,
    private val detector: NonDeterministicDetector
) : ClassVisitor(Settings.ASM_API, classVisitor) {

    private var currentClass: String? = null

    private fun getOwnerClass(owner: String): Class<*> =
        utContext.classLoader.loadClass(owner.replace('/', '.'))

    private fun getMethodId(owner: String?, name: String?, descriptor: String?): MethodId? {
        if (owner == null || name == null || descriptor == null) {
            return null
        }
        val clazz = getOwnerClass(owner)
        val method = clazz.methods.find {
            it.name == name && Method.getMethod(it).descriptor == descriptor
        }
        return method?.executableId
    }

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        currentClass = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = cv.visitMethod(access, name, descriptor, signature, exceptions)
        return object : MethodVisitor(Settings.ASM_API, mv) {
            override fun visitMethodInsn(
                opcodeAndSource: Int,
                owner: String?,
                name: String?,
                descriptor: String?,
                isInterface: Boolean
            ) {
                if (name == "<init>") {
                    mv.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
                    owner?.let {
                        if (detector.isNonDeterministic(getOwnerClass(it).id)) {
                            detector.inserter.insertAfterNDInstanceConstructor(mv, currentClass!!)
                        }
                    }
                    return
                }

                getMethodId(owner, name, descriptor)?.let { method ->
                    if (detector.isNonDeterministic(method)) {
                        detector.inserter.insertBeforeNDMethod(mv, method)
                        mv.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
                        detector.inserter.insertAfterNDMethod(mv, method)
                        return
                    }
                }

                mv.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
            }
        }
    }
}