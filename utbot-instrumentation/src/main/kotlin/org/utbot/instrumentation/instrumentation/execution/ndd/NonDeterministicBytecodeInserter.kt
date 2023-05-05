package org.utbot.instrumentation.instrumentation.execution.ndd

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.*

class NonDeterministicBytecodeInserter {
    private val internalName = Type.getInternalName(NonDeterministicResultStorage::class.java)

    private fun ClassId.descriptor(): String = when (this) {
        booleanClassId -> "Z"
        byteClassId -> "B"
        charClassId -> "C"
        shortClassId -> "S"
        intClassId -> "I"
        longClassId -> "J"
        floatClassId -> "F"
        doubleClassId -> "D"
        else -> "Ljava/lang/Object;"
    }

    private fun MethodId.toStoreDescriptor(): String = buildString {
        append('(')
        append(returnType.descriptor())
        append("Ljava/lang/String;)V")
    }

    private fun MethodVisitor.invoke(name: String, descriptor: String) {
        visitMethodInsn(Opcodes.INVOKESTATIC, internalName, name, descriptor, false)
    }

    fun insertAfterNDMethod(mv: MethodVisitor, methodId: MethodId) {
        mv.visitInsn(Opcodes.DUP)
        mv.visitLdcInsn(NonDeterministicResultStorage.methodToSignature(methodId))
        mv.invoke(if (methodId.isStatic) "storeStatic" else "storeCall", methodId.toStoreDescriptor())
    }

    fun insertBeforeNDMethod(mv: MethodVisitor, methodId: MethodId) {
        if (methodId.isStatic) {
            return
        }

        methodId.parameters.asReversed().forEach {
            mv.invoke("putParameter", "(${it.descriptor()})V")
        }

        mv.visitInsn(Opcodes.DUP)
        mv.invoke("saveInstance", "(Ljava/lang/Object;)V")

        methodId.parameters.forEach {
            mv.invoke("peakParameter", "()${it.descriptor()}")
        }
    }

    fun insertAfterNDInstanceConstructor(mv: MethodVisitor, callSite: String) {
        mv.visitInsn(Opcodes.DUP)
        mv.visitLdcInsn(callSite)
        mv.invoke("registerInstance", "(Ljava/lang/Object;Ljava/lang/String;)V")
    }
}