package org.utbot.instrumentation.instrumentation.mock

import java.lang.reflect.Method
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.instrumenter.visitors.util.FieldInitializer

object MockConfig {
    const val IS_MOCK_FIELD = "\$__is_mock_"
}

/**
 * ClassVisitor that instruments classes to mock new instruction calls and methods.
 */
class MockClassVisitor(
    classVisitor: ClassVisitor,
    private val mockGetter: Method,
    private val callSiteChecker: Method,
    private val hasMock: Method
) : ClassVisitor(Settings.ASM_API, classVisitor) {
    val signatureToId = mutableMapOf<String, Int>()

    private lateinit var internalClassName: String
    private val extraFields = mutableListOf<FieldInitializer>()

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
        // we do not want to <clinit> or synthetic methods
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
        val computedSignature = name + descriptor
        val id = signatureToId.size
        signatureToId[computedSignature] = id

        val isMockInitializer =
            StaticPrimitiveInitializer(internalClassName, MockConfig.IS_MOCK_FIELD + id, Type.BOOLEAN_TYPE)
        extraFields += isMockInitializer

        val mv = cv.visitMethod(access, name, descriptor, signature, exceptions)
        return MockMethodVisitor(
            mv, access, name, descriptor, computedSignature,
            internalClassName, mockGetter,
            callSiteChecker, hasMock, isMockInitializer
        )
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

