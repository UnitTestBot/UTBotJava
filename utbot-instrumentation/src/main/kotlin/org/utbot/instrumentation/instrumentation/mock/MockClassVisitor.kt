package org.utbot.instrumentation.instrumentation.mock

import java.lang.reflect.Method
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.commons.Method.getMethod
import org.utbot.instrumentation.Settings

class MockClassVisitor(
    classVisitor: ClassVisitor,
    mockGetter: Method,
    callSiteChecker: Method,
    hasMock: Method
) : ClassVisitor(Settings.ASM_API, classVisitor) {
    private lateinit var internalClassName: String

    private val mockGetterOwner = Type.getType(mockGetter.declaringClass)
    private val mockGetterMethod = getMethod(mockGetter)

    private val callSiteCheckerOwner = Type.getType(callSiteChecker.declaringClass)
    private val callSiteCheckerMethod = getMethod(callSiteChecker)

    private val hasMockOwner = Type.getType(hasMock.declaringClass)
    private val hasMockMethod = getMethod(hasMock)

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        internalClassName = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val isNotSynthetic = access.and(Opcodes.ACC_SYNTHETIC) == 0
        // we do not want to mock <init> or <clinit> or synthetic methods
        return if (name != "<clinit>" && name != "<init>" && isNotSynthetic) {
            visitMethodImpl(access, name, descriptor, signature, exceptions)
        } else {
            cv.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }

    private fun visitMethodImpl(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = cv.visitMethod(access, name, descriptor, signature, exceptions)
        return object : AdviceAdapter(Settings.ASM_API, mv, access, name, descriptor) {

            private val afterLabels: MutableList<Label> = mutableListOf()

            override fun visitTypeInsn(opcode: Int, type: String?) {
                if (opcode == NEW) {
                    val afterLabel = Label()
                    afterLabels.add(afterLabel)
                    val newLabel = Label()

                    // if (callSiteCheck(type, class))
                    push(type)
                    push(internalClassName)
                    invokeStatic(callSiteCheckerOwner, callSiteCheckerMethod)
                    ifZCmp(IFEQ, newLabel)

                    // if (hasMock(type))
                    push(type)
                    invokeStatic(hasMockOwner, hasMockMethod)
                    ifZCmp(IFEQ, newLabel)

                    // getMock(type)
                    push(type)
                    invokeStatic(mockGetterOwner, mockGetterMethod)
                    visitTypeInsn(CHECKCAST, type)
                    goTo(afterLabel)

                    // else
                    visitLabel(newLabel)
                }
                super.visitTypeInsn(opcode, type)
            }

            override fun visitMethodInsn(
                opcodeAndSource: Int,
                owner: String?,
                name: String?,
                descriptor: String?,
                isInterface: Boolean
            ) {
                super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
                if (name == "<init>") {
                    afterLabels.removeLastOrNull()?.let { visitLabel(it) }
                }
            }
        }
    }
}

