package org.utbot.instrumentation.instrumentation.mock

import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.instrumenter.visitors.util.FieldInitializer
import java.lang.reflect.Method
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.commons.Method.getMethod
import org.utbot.framework.plugin.api.util.signature

object MockConfig {
    const val IS_MOCK_FIELD = "\$__is_mock_"
}

/**
 * Computes key for method that is used for mocking.
 */
fun computeKeyForMethod(internalType: String, methodSignature: String) =
    "$internalType@$methodSignature"

fun computeKeyForMethod(method: Method) =
    computeKeyForMethod(Type.getInternalName(method.declaringClass), method.signature)

class MockClassVisitor(
    classVisitor: ClassVisitor,
    mockGetter: Method,
    callSiteChecker: Method,
    hasMock: Method
) : ClassVisitor(Settings.ASM_API, classVisitor) {
    val signatureToId = mutableMapOf<String, Int>()

    private lateinit var internalClassName: String
    private val extraFields = mutableListOf<FieldInitializer>()

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
        // we do not want to mock <clinit> or synthetic methods
        return if (name != "<clinit>" && isNotSynthetic) {
            visitStaticMethod(access, name, descriptor, signature, exceptions)
        } else {
            cv.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }

    private fun visitStaticMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val isStatic = access and Opcodes.ACC_STATIC != 0
        val isVoidMethod = Type.getReturnType(descriptor) == Type.VOID_TYPE

        val computedSignature = computeKeyForMethod(internalClassName, "$name$descriptor")
        val id = signatureToId.size
        signatureToId[computedSignature] = id

        val isMockInitializer =
            StaticPrimitiveInitializer(internalClassName, MockConfig.IS_MOCK_FIELD + id, Type.BOOLEAN_TYPE)
        extraFields += isMockInitializer

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
                    push(computedSignature)

                    invokeStatic(hasMockOwner, hasMockMethod)
                    ifZCmp(IFEQ, afterIfLabel)

                    if (isStatic) {
                        visitInsn(ACONST_NULL)
                    } else {
                        loadThis()
                    }
                    push(computedSignature)

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
    }

    override fun visitEnd() {
        extraFields.forEach { addField(it) }
        cv.visitEnd()
    }

    private fun addField(field: FieldInitializer) {
        val fv = cv.visitField(
            Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_SYNTHETIC + Opcodes.ACC_FINAL,
            field.name,
            field.descriptor,
            field.signature,
            null
        )
        fv.visitEnd()
    }
}

