package org.utbot.instrumentation.instrumentation.coverage

import org.jacoco.core.internal.flow.ClassProbesAdapter
import org.jacoco.core.internal.instr.createClassVisitorForBranchCoverageInstrumentation
import org.utbot.common.withAccessibility
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.instrumentation.util.CastProbeCounterException
import org.utbot.instrumentation.util.NoProbeCounterException
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

        val cv = instrumenter.visitClass { writer ->
            createClassVisitorForBranchCoverageInstrumentation(writer, className)
        }

        val probeCounterField = ClassProbesAdapter::class.java.declaredFields.firstOrNull { it.name == "counter" }
            ?: throw NoProbeCounterException(ClassProbesAdapter::class.java, "counter")
        val probeCounter = probeCounterField.withAccessibility {
            this.get(cv) as? Int ?: throw CastProbeCounterException()
        }

        instrumenter.addStaticField(StaticArrayFieldInitializer(className, Settings.PROBES_ARRAY_NAME, probeCounter))

        return instrumenter.classByteCode
    }

    object Factory : Instrumentation.Factory<Result<*>, BranchCoverageInstrumentation> {
        override fun create(): BranchCoverageInstrumentation = BranchCoverageInstrumentation()
    }
}
