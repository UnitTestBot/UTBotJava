package org.utbot.instrumentation.instrumentation.coverage

import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import java.security.ProtectionDomain

/**
 * This instrumentation allows collecting instruction coverage after several calls.
 */
open class InstructionCoverageInstrumentation : CoverageInstrumentation() {

    override fun <T : Any> computeMapOfRanges(clazz: Class<out T>): Map<String, IntRange> {
        return Instrumenter(clazz).computeMapOfRangesForInstructionCoverage()
    }

    /**
     * Transforms bytecode such way that it becomes possible to get an instruction coverage.
     *
     * Adds set of instructions which marks the executed instruction as completed. Uses static boolean array for this.
     */
    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray {
        val instrumenter = Instrumenter(classfileBuffer)

        val staticArrayStrategy = StaticArrayStrategy(className, Settings.PROBES_ARRAY_NAME)
        instrumenter.visitInstructions(staticArrayStrategy)
        instrumenter.addStaticField(staticArrayStrategy)

        return instrumenter.classByteCode
    }

    object Factory : Instrumentation.Factory<Result<*>, InstructionCoverageInstrumentation> {
        override fun create(): InstructionCoverageInstrumentation = InstructionCoverageInstrumentation()
    }
}