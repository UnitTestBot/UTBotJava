package org.utbot.instrumentation.instrumentation.execution.phases

import org.objectweb.asm.Type
import org.utbot.framework.plugin.api.*
import org.utbot.instrumentation.instrumentation.et.EtInstruction
import org.utbot.instrumentation.instrumentation.et.TraceHandler
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult

/**
 * This phase is about collection statistics such as coverage.
 */
class StatisticsCollectionContext(
    private val traceHandler: TraceHandler
) : ExecutionPhase {

    override fun wrapError(e: Throwable): ExecutionPhaseException {
        val message = this.javaClass.simpleName
        return when(e) {
            is TimeoutException ->  ExecutionPhaseStop(message, UtConcreteExecutionResult(MissingState, UtTimeoutException(e), Coverage()))
            else -> ExecutionPhaseError(message, e)
        }
    }

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