package org.jacoco.core.internal.instr

import org.jacoco.core.internal.flow.ClassProbesAdapter
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.et.ProcessingStorage

fun createClassVisitorForBranchCoverageInstrumentation(
    writer: ClassWriter,
    className: String
): ClassProbesAdapter {
    val strategy = ClassFieldProbeArrayStrategy(className)
    return ClassProbesAdapter(
        ClassInstrumenter(strategy, writer),
        false
    )
}

class MethodProbesCollector(
    strategy: IProbeArrayStrategy,
    writer: ClassVisitor,
    val methodToProbes: MutableMap<String, MutableList<Int>> = mutableMapOf()
) : ClassVisitor(
    Settings.ASM_API,
    ClassProbesAdapter(
        NoneClassInstrumenter(strategy, writer, methodToProbes),
        false
    )
)

fun createClassVisitorForComputeMapOfRangesForBranchCoverage(
    writer: ClassWriter
): MethodProbesCollector {
    val strategy = NoneProbeArrayStrategy()
    return MethodProbesCollector(strategy, writer)
}

fun createClassVisitorForTracingBranchInstructions(
    className: String,
    storage: ProcessingStorage,
    writer: ClassWriter
): ClassProbesAdapter {
    val strategy = TraceStrategy()
    return ClassProbesAdapter(
        TraceClassInstrumenter(strategy, writer, storage) { localId ->
            storage.computeId(className, localId)
        },
        false
    )
}