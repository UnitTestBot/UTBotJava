package org.jacoco.core.internal.instr

import org.jacoco.core.internal.flow.ClassProbesAdapter
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.TraceClassVisitor
import org.utbot.instrumentation.Settings
import java.io.PrintWriter
import java.io.StringWriter

val sw: StringWriter = StringWriter()

fun createJacocoClassVisitorForCollectionBranchCoverage(writer: ClassWriter): MyMethodToProbesVisitor {
    val strategy = NoneProbeArrayStrategy()
    val tcv = TraceClassVisitor(writer, PrintWriter(sw))
    return MyMethodToProbesVisitor(Settings.ASM_API, strategy, tcv)
}

fun createJacocoClassVisitorForBytecodeInstrumentation(writer: ClassWriter, className: String): ClassProbesAdapter {
    val strategy = MyClassFieldProbeArrayStrategy(className)
    return ClassProbesAdapter(
        ClassInstrumenter(strategy, writer),
        true
    )
}