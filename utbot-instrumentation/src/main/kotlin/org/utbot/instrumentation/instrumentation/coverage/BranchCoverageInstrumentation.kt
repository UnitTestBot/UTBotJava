package org.utbot.instrumentation.instrumentation.coverage

import org.jacoco.core.internal.flow.ClassProbesAdapter
import org.jacoco.core.internal.instr.createClassVisitorForBranchCoverageInstrumentation
import org.jacoco.core.internal.instr.sw
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.utbot.common.withAccessibility
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.instrumenter.ClassVisitorBuilder
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import java.security.ProtectionDomain

class BranchCoverageInstrumentation : CoverageInstrumentation() {

    override fun <T : Any> computeMapOfRanges(clazz: Class<out T>): Map<String, IntRange> {
        return Instrumenter(clazz).computeMapOfRangesForBranchCoverage()
    }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray {
        val instrumenter = Instrumenter(classfileBuffer)

        val cv = instrumenter.visitClass(object : ClassVisitorBuilder<ClassProbesAdapter> {
            override val writerFlags: Int
                get() = 0

            override val readerParsingOptions: Int
                get() = ClassReader.EXPAND_FRAMES

            override fun build(writer: ClassWriter): ClassProbesAdapter =
                createClassVisitorForBranchCoverageInstrumentation(writer, className)
        })

        // TODO exceptions
        val probesCountField = ClassProbesAdapter::class.java.declaredFields.firstOrNull { it.name == "counter" }
            ?: throw IllegalStateException()
        val probeCount = probesCountField.withAccessibility {
            this.get(cv) as? Int ?: throw ClassCastException()
        }

        instrumenter.addStaticField(StaticArrayFieldInitializer(className, Settings.PROBES_ARRAY_NAME, probeCount))

        val result = sw.toString()
        println(result)

        return instrumenter.classByteCode
    }

    object Factory : Instrumentation.Factory<Result<*>, BranchCoverageInstrumentation> {
        override fun create(): BranchCoverageInstrumentation = BranchCoverageInstrumentation()
    }
}
