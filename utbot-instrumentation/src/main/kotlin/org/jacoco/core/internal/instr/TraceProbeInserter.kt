package org.jacoco.core.internal.instr

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.utbot.instrumentation.instrumentation.et.RuntimeTraceStorage
import kotlin.reflect.jvm.javaMethod

internal class TraceProbeInserter(
    access: Int,
    name: String,
    desc: String,
    mv: MethodVisitor,
    arrayStrategy: IProbeArrayStrategy,
    private val probeIdGenerator: ProbeIdGenerator,
) : ProbeInserter(access, name, desc, mv, arrayStrategy) {

    private val internalName = Type.getInternalName(RuntimeTraceStorage::class.java)
    private val visitMethodDescriptor = Type.getMethodDescriptor(RuntimeTraceStorage::visit.javaMethod)
    private val variable: Int

    init {
        var pos = if (Opcodes.ACC_STATIC and access == 0) 1 else 0
        for (t in Type.getArgumentTypes(desc)) {
            pos += t.size
        }
        variable = pos
    }

    override fun insertProbe(ignored: Int) {
        mv.visitVarInsn(Opcodes.ILOAD, variable)
        val id = probeIdGenerator.nextId()
        mv.visitLdcInsn(id)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, internalName, "visit", visitMethodDescriptor, false)
    }

    override fun visitCode() {
        super.visitCode()
        insertProbe(0)
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        insertProbe(0)
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

}