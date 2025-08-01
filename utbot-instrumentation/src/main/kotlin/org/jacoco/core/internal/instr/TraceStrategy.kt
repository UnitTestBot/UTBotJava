package org.jacoco.core.internal.instr

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.utbot.instrumentation.instrumentation.et.RuntimeTraceStorage
import kotlin.reflect.jvm.javaField

class TraceStrategy : IProbeArrayStrategy {

    private val internalName = Type.getInternalName(RuntimeTraceStorage::class.java)
    private val counterCallIdName = RuntimeTraceStorage::`$__counter_call_id__`.javaField!!.name

    override fun storeInstance(mv: MethodVisitor, clinit: Boolean, variable: Int): Int {
        // int variable = RuntimeTraceStorage.$__counter_call_id__++
        mv.visitFieldInsn(Opcodes.GETSTATIC, internalName, counterCallIdName, RuntimeTraceStorage.DESC_CALL_ID_COUNTER)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitInsn(Opcodes.IADD)
        mv.visitInsn(Opcodes.DUP)
        mv.visitFieldInsn(Opcodes.PUTSTATIC, internalName, counterCallIdName, RuntimeTraceStorage.DESC_CALL_ID_COUNTER)
        mv.visitVarInsn(Opcodes.ISTORE, variable)
        return 2
    }

    override fun addMembers(cv: ClassVisitor?, probeCount: Int) {
        // nothing to do
    }

}