package org.utbot.instrumentation.instrumentation.mock

import java.lang.reflect.Method
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.commons.Method.getMethod
import org.utbot.instrumentation.Settings

/**
 * MethodVisitor that instruments method's body to mock new instruction calls and methods.
 */
internal class MockMethodVisitor(
    mv: MethodVisitor?,
    access: Int,
    name: String,
    descriptor: String,
    private val signature: String,
    private val internalClassName: String,
    mockGetter: Method,
    callSiteChecker: Method,
    hasMock: Method,
    private val isMockInitializer: StaticPrimitiveInitializer
) : AdviceAdapter(Settings.ASM_API, mv, access, name, descriptor) {

    private val isStatic = access and Opcodes.ACC_STATIC != 0
    private val isVoidMethod = Type.getReturnType(descriptor) == Type.VOID_TYPE
    private val isConstructor = name == "<init>"

    private val mockGetterOwner = Type.getType(mockGetter.declaringClass)
    private val mockGetterMethod = getMethod(mockGetter)

    private val callSiteCheckerOwner = Type.getType(callSiteChecker.declaringClass)
    private val callSiteCheckerMethod = getMethod(callSiteChecker)

    private val hasMockOwner = Type.getType(hasMock.declaringClass)
    private val hasMockMethod = getMethod(hasMock)

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

            // if (hasMock(null, type.<init>))
            visitInsn(ACONST_NULL)
            push("$type.<init>")
            invokeStatic(hasMockOwner, hasMockMethod)
            ifZCmp(IFEQ, newLabel)

            // getMock(null, type.<init>)
            visitInsn(ACONST_NULL)
            push("$type.<init>")
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

    override fun onMethodEnter() {
        // we don't want to mock constructor method
        if (isConstructor) return

        val afterIfLabel = Label()

        visitFieldInsn(
            GETSTATIC,
            internalClassName,
            isMockInitializer.name,
            isMockInitializer.descriptor
        )
        ifZCmp(IFEQ, afterIfLabel)

        if (isVoidMethod) {
            visitInsn(RETURN)
        } else {
            if (isStatic) {
                visitInsn(ACONST_NULL)
            } else {
                loadThis()
            }
            push(signature)

            invokeStatic(hasMockOwner, hasMockMethod)
            ifZCmp(IFEQ, afterIfLabel)

            if (isStatic) {
                visitInsn(ACONST_NULL)
            } else {
                loadThis()
            }
            push(signature)

            invokeStatic(mockGetterOwner, mockGetterMethod)

            if (returnType.sort == Type.OBJECT || returnType.sort == Type.ARRAY) {
                checkCast(returnType)
            } else { // primitive here
                unbox(returnType)
            }
            returnValue()
        }

        visitLabel(afterIfLabel)
    }
}