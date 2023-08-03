package org.jacoco.core.internal.instr

import org.jacoco.core.internal.flow.ClassProbesAdapter
import org.objectweb.asm.ClassVisitor

class MyMethodToProbesVisitor(
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