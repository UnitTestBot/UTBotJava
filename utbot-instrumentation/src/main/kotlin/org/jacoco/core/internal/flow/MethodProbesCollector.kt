package org.jacoco.core.internal.flow

import org.jacoco.core.internal.instr.IProbeArrayStrategy
import org.jacoco.core.internal.instr.NoneClassInstrumenter
import org.objectweb.asm.ClassVisitor

class MethodProbesCollector(
    api: Int,
    strategy: IProbeArrayStrategy,
    writer: ClassVisitor,
    val methodToProbes: MutableMap<String, MutableList<Int>> = mutableMapOf()
) : ClassVisitor(
    api,
    ClassProbesAdapter(
        NoneClassInstrumenter(strategy, writer, methodToProbes),
        true
    )
)