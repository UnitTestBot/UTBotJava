package org.jacoco.core.internal.instr

import org.jacoco.core.internal.flow.ClassProbesAdapter
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter
import java.io.StringWriter

val sw: StringWriter = StringWriter()

fun createJacocoClassVisitorForCollectionBranchCoverage(writer: ClassWriter): ClassVisitor {
    val strategy = NoneProbeArrayStrategy()
    val tcv = TraceClassVisitor(writer, PrintWriter(sw))
    return ClassProbesAdapter(
        MyClassInstrumenter(strategy, tcv),
        true
    )
}

fun createJacocoClassVisitorForBytecodeInstrumentation(writer: ClassWriter, className: String): ClassVisitor {
    val strategy = MyClassFieldProbeArrayStrategy(className)
    val tcv = TraceClassVisitor(writer, PrintWriter(sw))
    return ClassProbesAdapter(
        ClassInstrumenter(strategy, tcv),
        true
    )
}