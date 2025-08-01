package org.jacoco.core.internal.instr

import org.objectweb.asm.MethodVisitor
import org.utbot.instrumentation.Settings

/**
 * This inserter does not emit any code at all. This is used to collect method probes.
 */
class NoneProbeInserter(
    methodName: String,
    descriptor: String,
    private val methodToProbes: MutableMap<String, MutableList<Int>>,
    mv: MethodVisitor,
) : IProbeInserter, MethodVisitor(Settings.ASM_API, mv) {

    private val currentMethodSignature: String = methodName + descriptor

    override fun insertProbe(id: Int) {
        methodToProbes.getOrPut(currentMethodSignature) { mutableListOf() }.add(id)
    }

}