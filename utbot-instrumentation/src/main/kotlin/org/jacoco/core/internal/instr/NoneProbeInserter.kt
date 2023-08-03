package org.jacoco.core.internal.instr

import org.objectweb.asm.MethodVisitor
import org.utbot.instrumentation.Settings

/**
 * The probe inserter does not emit any code at all. This is used to collect method probes.
 */
class NoneProbeInserter(
    private val methodName: String,
    private val methodToProbes: MutableMap<String, MutableList<Int>>,
    mv: MethodVisitor,
) : IProbeInserter, MethodVisitor(Settings.ASM_API, mv) {

    override fun insertProbe(id: Int) {
        methodToProbes.getOrPut(methodName) { mutableListOf() }.add(id)
    }

}