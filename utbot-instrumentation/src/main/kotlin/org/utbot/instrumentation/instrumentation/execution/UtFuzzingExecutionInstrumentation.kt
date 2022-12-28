package org.utbot.framework.concrete

import org.objectweb.asm.Type
import org.utbot.framework.concrete.constructors.ConstructOnlyUserClassesOrCachedObjectsStrategy
import org.utbot.framework.concrete.mock.InstrumentationContext
import org.utbot.framework.concrete.phases.PhaseError
import org.utbot.framework.concrete.phases.PhasesController
import org.utbot.framework.concrete.phases.start
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.greyboxfuzzer.util.UtFuzzingConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.instrumentation.et.TraceHandler
import java.security.AccessControlException

object UtFuzzingExecutionInstrumentation : UtExecutionInstrumentationWithStatsCollection {

    override val delegateInstrumentation: InvokeInstrumentation = InvokeInstrumentation()

    override val instrumentationContext: InstrumentationContext = InstrumentationContext()

    override val traceHandler: TraceHandler = TraceHandler()
    override val pathsToUserClasses: MutableSet<String> = mutableSetOf<String>()
    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?
    ): UtFuzzingConcreteExecutionResult {
        if (parameters !is UtConcreteExecutionData) {
            throw IllegalArgumentException("Argument parameters must be of type UtConcreteExecutionData, but was: ${parameters?.javaClass}")
        }
        val (stateBefore, instrumentations, timeout) = parameters // smart cast to UtConcreteExecutionData
        return PhasesController(
            instrumentationContext,
            traceHandler,
            delegateInstrumentation,
            generateNullOnError = true
        ).computeFuzzingConcreteExecutionResult {
            // construction
            val (params, statics, cache) = valueConstructionContext.start {
                val params = constructParameters(stateBefore)
                val statics = constructStatics(stateBefore)

                mock(instrumentations)

                Triple(params, statics, getCache())
            }

            // preparation
            val savedStatics = preparationContext.start {
                val savedStatics = setStaticFields(statics)
                resetTrace()
                savedStatics
            }

            try {
                // invocation
                val concreteResult = invocationContext.start {
                    invoke(clazz, methodSignature, params.map { it.value }, timeout)
                }

                // statistics collection
                val coverage = statisticsCollectionContext.start { getCoverage(clazz) }.toLocalCoverage(traceHandler)
                val classJVMName = Type.getInternalName(clazz)
                val methodInstructions =
                    traceHandler.processingStorage.getMethodInstructions(classJVMName, methodSignature)

                val concreteUtModelResult = concreteResult.fold({
                    UtExecutionSuccess(UtNullModel(it?.javaClass?.id ?: objectClassId))
                }) {
                    sortOutException(it)
                }


                UtFuzzingConcreteExecutionResult(
                    null,
                    concreteUtModelResult,
                    coverage,
                    methodInstructions
                )

            } finally {
                // postprocessing
                postprocessingContext.start {
                    resetStaticFields(savedStatics)
                }
            }
        }
    }
}

object UtFuzzingExecutionInstrumentationWithStateAfterCollection : UtExecutionInstrumentationWithStatsCollection {

    override val delegateInstrumentation: InvokeInstrumentation = InvokeInstrumentation()

    override val instrumentationContext: InstrumentationContext = InstrumentationContext()

    override val traceHandler: TraceHandler = TraceHandler()
    override val pathsToUserClasses: MutableSet<String> = mutableSetOf()

    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?
    ): UtFuzzingConcreteExecutionResult {
        if (parameters !is UtConcreteExecutionData) {
            throw IllegalArgumentException("Argument parameters must be of type UtConcreteExecutionData, but was: ${parameters?.javaClass}")
        }
        val (stateBefore, instrumentations, timeout) = parameters // smart cast to UtConcreteExecutionData
        return PhasesController(
            instrumentationContext,
            traceHandler,
            delegateInstrumentation,
            generateNullOnError = true
        ).computeFuzzingConcreteExecutionResult {
            // construction
            val (params, statics, cache) = valueConstructionContext.start {
                val params = constructParameters(stateBefore)
                val statics = constructStatics(stateBefore)

                mock(instrumentations)

                Triple(params, statics, getCache())
            }

            // preparation
            val savedStatics = preparationContext.start {
                val savedStatics = setStaticFields(statics)
                resetTrace()
                savedStatics
            }

            try {
                // invocation
                val concreteResult = invocationContext.start {
                    invoke(clazz, methodSignature, params.map { it.value }, timeout)
                }

                // statistics collection
                val coverage = statisticsCollectionContext.start { getCoverage(clazz) }.toLocalCoverage(traceHandler)
                val classJVMName = Type.getInternalName(clazz)
                val methodInstructions =
                    traceHandler.processingStorage.getMethodInstructions(classJVMName, methodSignature)

                val methodId = clazz.singleExecutableId(methodSignature)
                val returnClassId = methodId.returnType
                // model construction
                val (executionResult, stateAfter) = modelConstructionContext.start {
                    configureConstructor {
                        this.cache = cache
                        strategy = ConstructOnlyUserClassesOrCachedObjectsStrategy(pathsToUserClasses, cache)
                    }

                    val executionResult = convertToExecutionResult(concreteResult, returnClassId)

                    val stateAfterParametersWithThis = constructParameters(params)
                    val stateAfterStatics = constructStatics(stateBefore, statics)
                    val (stateAfterThis, stateAfterParameters) = if (stateBefore.thisInstance == null) {
                        null to stateAfterParametersWithThis
                    } else {
                        stateAfterParametersWithThis.first() to stateAfterParametersWithThis.drop(1)
                    }
                    val stateAfter = EnvironmentModels(stateAfterThis, stateAfterParameters, stateAfterStatics)

                    executionResult to stateAfter
                }
                UtFuzzingConcreteExecutionResult(
                    stateAfter,
                    executionResult,
                    coverage,
                    methodInstructions
                )
            } finally {
                // postprocessing
                postprocessingContext.start {
                    resetStaticFields(savedStatics)
                }
            }
        }
    }
}

private fun Coverage.toLocalCoverage(traceHandler: TraceHandler) =
    Coverage(
        coveredInstructions.map { traceHandler.processingStorage.convertToLocalInstruction(it) },
        instructionsCount,
        missedInstructions.map { traceHandler.processingStorage.convertToLocalInstruction(it) }
    )

private inline fun PhasesController.computeFuzzingConcreteExecutionResult(block: PhasesController.() -> UtFuzzingConcreteExecutionResult): UtFuzzingConcreteExecutionResult {
    return use {
        try {
            block()
        } catch (e: PhaseError) {
            if (e.cause.cause is AccessControlException) {
                return@use UtFuzzingConcreteExecutionResult(
                    MissingState,
                    UtSandboxFailure(e.cause.cause!!),
                    Coverage(),
                    null
                )
            }
            throw e
        }
    }
}
