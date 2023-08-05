package org.jacoco.core.internal.instr

import org.jacoco.core.internal.flow.ClassProbesAdapter
import org.jacoco.core.internal.flow.MethodProbesCollector
import org.jacoco.core.internal.flow.TraceClassProbesAdapter
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.TraceClassVisitor
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.et.ProcessingStorage
import java.io.PrintWriter
import java.io.StringWriter

// TODO: remove
val sw: StringWriter = StringWriter()

fun createClassVisitorForBranchCoverageInstrumentation(writer: ClassWriter, className: String): ClassProbesAdapter {
    val strategy = ClassFieldProbeArrayStrategy(className)
    val tcv = TraceClassVisitor(writer, PrintWriter(sw)) // TODO: remove
    return ClassProbesAdapter(
        ClassInstrumenter(strategy, tcv),
        false
    )
}

fun createClassVisitorForComputeMapOfRangesForBranchCoverage(writer: ClassWriter): MethodProbesCollector {
    val strategy = NoneProbeArrayStrategy()
    val tcv = TraceClassVisitor(writer, PrintWriter(sw)) // TODO: remove
    return MethodProbesCollector(Settings.ASM_API, strategy, tcv)
}

fun createClassVisitorForTracingBranchInstructions(
    className: String,
    storage: ProcessingStorage,
    writer: ClassWriter
): TraceClassProbesAdapter {
    val strategy = TraceStrategy()
    val tcv = TraceClassVisitor(writer, PrintWriter(sw)) // TODO: remove
    return TraceClassProbesAdapter(
        TraceClassInstrumenter(strategy, tcv) { id ->
            storage.computeId(className, id)
        },
        className,
        storage
    )
}