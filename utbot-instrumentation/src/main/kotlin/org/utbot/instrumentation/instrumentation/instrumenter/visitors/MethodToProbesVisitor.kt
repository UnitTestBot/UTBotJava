package org.utbot.instrumentation.instrumentation.instrumenter.visitors

import org.utbot.instrumentation.instrumentation.instrumenter.visitors.util.IInstructionVisitor
import org.objectweb.asm.MethodVisitor

// TODO: document this

class MethodToProbesVisitor : IInstructionVisitor {
    lateinit var currentMethodSignature: String

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?,
        methodVisitor: MethodVisitor
    ) {
        currentMethodSignature = name + descriptor
    }

    override fun visitInstruction(mv: MethodVisitor): MethodVisitor {
        methodToProbes.getOrPut(currentMethodSignature) { mutableListOf() }.add(probeId)
        nextProbeId()
        return mv
    }

    val methodToProbes = mutableMapOf<String, MutableList<Int>>()

    private var probeId = 0

    private fun nextProbeId(): Int {
        return probeId++
    }
}