package org.utbot.instrumentation.instrumentation.execution.phases

import org.objectweb.asm.Type
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.Instruction
import org.utbot.instrumentation.instrumentation.et.EtInstruction
import org.utbot.instrumentation.instrumentation.et.TraceHandler

class StatisticsCollectionPhaseError(cause: Throwable) : PhaseError(
    message = "Error during statistics collection phase",
    cause
)

/**
 * This phase is about collection statistics such as coverage.
 */
class StatisticsCollectionContext(
    private val traceHandler: TraceHandler
) : PhaseContext<StatisticsCollectionPhaseError> {

    override fun wrapError(error: Throwable): StatisticsCollectionPhaseError =
        StatisticsCollectionPhaseError(error)

    fun getCoverage(clazz: Class<*>): Coverage {
        return traceHandler
            .computeInstructionList()
            .toApiCoverage(
                traceHandler.processingStorage.getInstructionsCount(
                    Type.getInternalName(clazz)
                )
            )
    }

    /**
     * Transforms a list of internal [EtInstruction]s to a list of api [Instruction]s.
     */
    private fun List<EtInstruction>.toApiCoverage(instructionsCount: Long? = null): Coverage =
        Coverage(
            map { Instruction(it.className, it.methodSignature, it.line, it.id) },
            instructionsCount
        )
}