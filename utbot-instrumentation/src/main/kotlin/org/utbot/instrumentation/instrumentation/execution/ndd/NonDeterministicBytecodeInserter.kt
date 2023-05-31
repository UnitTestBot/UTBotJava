package org.utbot.instrumentation.instrumentation.execution.ndd

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class NonDeterministicBytecodeInserter {
    private val internalName = Type.getInternalName(NonDeterministicResultStorage::class.java)

    private fun String.getUnifiedParamsTypes(): List<String> {
        val list = mutableListOf<String>()
        var readObject = false
        for (c in this) {
            if (c == '(') {
                continue
            }
            if (c == ')') {
                break
            }
            if (readObject) {
                if (c == ';') {
                    readObject = false
                    list.add("Ljava/lang/Object;")
                }
            } else if (c == 'L') {
                readObject = true
            } else {
                list.add(c.toString())
            }
        }

        return list
    }

    private fun String.unifyTypeDescriptor(): String =
        if (startsWith('L')) {
            "Ljava/lang/Object;"
        } else {
            this
        }

    private fun String.getReturnType(): String =
        substringAfter(')')

    private fun getStoreDescriptor(descriptor: String): String = buildString {
        append('(')
        append(descriptor.getReturnType().unifyTypeDescriptor())
        append("Ljava/lang/String;)V")
    }

    private fun MethodVisitor.invoke(name: String, descriptor: String) {
        visitMethodInsn(Opcodes.INVOKESTATIC, internalName, name, descriptor, false)
    }

    fun insertAfterNDMethod(mv: MethodVisitor, owner: String, name: String, descriptor: String, isStatic: Boolean) {
        mv.visitInsn(Opcodes.DUP)
        mv.visitLdcInsn(NonDeterministicResultStorage.makeSignature(owner, name, descriptor))
        mv.invoke(if (isStatic) "storeStatic" else "storeCall", getStoreDescriptor(descriptor))
    }

    fun insertBeforeNDMethod(mv: MethodVisitor, descriptor: String, isStatic: Boolean) {
        if (isStatic) {
            return
        }

        val params = descriptor.getUnifiedParamsTypes()

        params.asReversed().forEach {
            mv.invoke("putParameter${it[0]}", "($it)V")
        }

        mv.visitInsn(Opcodes.DUP)
        mv.invoke("saveInstance", "(Ljava/lang/Object;)V")

        params.forEach {
            mv.invoke("peakParameter${it[0]}", "()$it")
        }
    }

    fun insertAfterNDInstanceConstructor(mv: MethodVisitor, callSite: String) {
        mv.visitInsn(Opcodes.DUP)
        mv.visitLdcInsn(callSite)
        mv.invoke("registerInstance", "(Ljava/lang/Object;Ljava/lang/String;)V")
    }
}