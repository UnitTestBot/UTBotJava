package org.jacoco.core.internal.instr

import org.objectweb.asm.MethodVisitor

class MyProbeInserter(
    private val name: String,
    mv: MethodVisitor,
) : IProbeInserter, MethodVisitor(InstrSupport.ASM_API_VERSION, mv) {

    companion object {
        val methodToProbes = mutableMapOf<String, MutableList<Int>>()
    }

    override fun insertProbe(id: Int) {
        methodToProbes.getOrPut(name) { mutableListOf() }.add(id)
    }

}