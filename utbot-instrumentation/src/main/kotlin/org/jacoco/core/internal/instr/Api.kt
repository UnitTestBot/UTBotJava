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
    val strategy = ProbeArrayStrategy(className)
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

class ProbeIdGenerator(private val f: (Int) -> Long) {

    private var localId: Int = 0

    fun currentId(): Long = f(localId)

    fun nextId(): Long = f(localId++)

}

fun createClassVisitorForTracingBranchInstructions(
    className: String,
    storage: ProcessingStorage,
    writer: ClassWriter
): ClassProbesAdapter {
    val strategy = TraceStrategy()
    val probeIdGenerator = ProbeIdGenerator { localId ->
        storage.computeId(className, localId)
    }
    return ClassProbesAdapter(
        TraceClassInstrumenter(strategy, writer, className, storage, probeIdGenerator),
        false
    )
}