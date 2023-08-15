package org.utbot.instrumentation.instrumentation.execution.phases

import org.objectweb.asm.Type
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.instrumentation.instrumentation.et.EtInstruction
import org.utbot.instrumentation.instrumentation.et.TraceHandler
import org.utbot.instrumentation.instrumentation.execution.PreliminaryUtConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.execution.ndd.NonDeterministicResultStorage
import java.util.*

/**
 * This phase is about collection statistics such as coverage.
 */
class StatisticsCollectionPhase(
    private val traceHandler: TraceHandler
) : ExecutionPhase {

    override fun wrapError(e: Throwable): ExecutionPhaseException {
        val message = this.javaClass.simpleName
        return when (e) {
            is TimeoutException -> ExecutionPhaseStop(
                message,
                PreliminaryUtConcreteExecutionResult(
                    stateAfter = MissingState,
                    result = UtTimeoutException(e),
                    coverage = Coverage()
                )
            )

            else -> ExecutionPhaseError(message, e)
        }
    }

    data class NDResults(
        val statics: Map<MethodId, List<Any?>>,
        val news: Map<ClassId, Pair<List<Any>, Set<ClassId>>>,
        val calls: IdentityHashMap<Any, Map<MethodId, List<Any?>>>
    )

    fun getNonDeterministicResults(): NDResults {
        val storage = NonDeterministicResultStorage

        val statics = storage.staticStorage
            .groupBy { storage.signatureToMethod(it.signature)!! }
            .mapValues { (_, values) -> values.map { it.result } }

        val news = storage.ndInstances.entries
            .groupBy { it.key.javaClass.id }
            .mapValues { (_, entries) ->
                val values = entries.sortedBy { it.value.instanceNumber }.map { it.key }
                val callSites = entries.map {
                    utContext.classLoader.loadClass(it.value.callSite.replace('/', '.')).id
                }.toSet()
                values to callSites
            }

        val calls = storage.callStorage
            .mapValuesTo(IdentityHashMap()) { (_, methodResults) ->
                methodResults
                    .groupBy { storage.signatureToMethod(it.signature)!! }
                    .mapValues { (_, values) -> values.map { it.result } }
            }

        return NDResults(statics, news, calls)
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