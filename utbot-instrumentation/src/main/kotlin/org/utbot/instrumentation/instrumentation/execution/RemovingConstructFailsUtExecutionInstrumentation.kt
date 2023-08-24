package org.utbot.instrumentation.instrumentation.execution

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtConcreteExecutionProcessedFailure
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtStatementCallModel
import org.utbot.framework.plugin.api.isNull
import org.utbot.framework.plugin.api.mapper.UtModelDeepMapper
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.process.kryo.KryoHelper
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.execution.context.InstrumentationContext
import org.utbot.instrumentation.instrumentation.execution.phases.ExecutionPhaseStop
import org.utbot.instrumentation.instrumentation.execution.phases.PhasesController
import org.utbot.instrumentation.instrumentation.execution.phases.ValueConstructionPhase
import org.utbot.instrumentation.process.generated.InstrumentedProcessModel
import org.utbot.rd.IdleWatchdog
import java.security.ProtectionDomain

/**
 * [UtExecutionInstrumentation] that on [invoke] tries to run [invoke] of the [delegateInstrumentation]
 * a few times, each time removing failing [UtStatementCallModel]s, until either max number of reruns
 * is reached or [invoke] of the [delegateInstrumentation] no longer fails with [UtConcreteExecutionProcessedFailure].
 *
 * @see [UtStatementCallModel.thrownConcreteException]
 */
class RemovingConstructFailsUtExecutionInstrumentation(
    instrumentationContext: InstrumentationContext,
    delegateInstrumentationFactory: UtExecutionInstrumentation.Factory<*>
) : UtExecutionInstrumentation {
    companion object {
        private const val MAX_RETRIES = 5
        private val logger = getLogger<RemovingConstructFailsUtExecutionInstrumentation>()
    }

    private val delegateInstrumentation = delegateInstrumentationFactory.create(object : InstrumentationContext by instrumentationContext {
        override fun handleLastCaughtConstructionException(exception: Throwable) {
            throw ExecutionPhaseStop(
                phase = ValueConstructionPhase::class.java.simpleName,
                result = PreliminaryUtConcreteExecutionResult(
                    stateAfter = MissingState,
                    result = UtConcreteExecutionProcessedFailure(exception),
                    coverage = Coverage()
                )
            )
        }
    })
    private var runsCompleted = 0
    private var nextRunIndexToLog = 1 // we log `attemptsDistribution` every run that has index that is a power of 10
    private val attemptsDistribution = mutableMapOf<Int, Int>()

    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?,
        phasesWrapper: PhasesController.(invokeBasePhases: () -> PreliminaryUtConcreteExecutionResult) -> PreliminaryUtConcreteExecutionResult
    ): UtConcreteExecutionResult {
        @Suppress("NAME_SHADOWING")
        var parameters = parameters as UtConcreteExecutionData
        var attempt = 0
        var res: UtConcreteExecutionResult
        try {
            do {
                res = delegateInstrumentation.invoke(clazz, methodSignature, arguments, parameters, phasesWrapper)

                if (res.result !is UtConcreteExecutionProcessedFailure)
                    return res

                parameters = parameters.mapModels(UtModelDeepMapper.fromSimpleShallowMapper { model ->
                    shallowlyRemoveFailingCalls(model)
                })

                // if `thisInstance` is present and became `isNull`, then we should stop trying to
                // correct this execution and return `UtConcreteExecutionProcessedFailure`
                if (parameters.stateBefore.thisInstance?.isNull() == true)
                    return res

            } while (attempt++ < MAX_RETRIES)

            return res
        } finally {
            runsCompleted++
            attemptsDistribution[attempt] = (attemptsDistribution[attempt] ?: 0) + 1
            if (runsCompleted == nextRunIndexToLog) {
                nextRunIndexToLog *= 10
                logger.info { "Run: $runsCompleted, attemptsDistribution: $attemptsDistribution" }
            }
        }
    }

    private fun shallowlyRemoveFailingCalls(model: UtModel): UtModel = when {
        model !is UtAssembleModel -> model
        model.instantiationCall.thrownConcreteException != null -> model.classId.defaultValueModel()
        else -> UtAssembleModel(
            id = model.id,
            classId = model.classId,
            modelName = model.modelName,
            instantiationCall = model.instantiationCall,
            origin = model.origin,
            modificationsChainProvider = {
                model.modificationsChain.filter {
                    (it as? UtStatementCallModel)?.thrownConcreteException == null &&
                            (it.instance as? UtAssembleModel)?.instantiationCall?.thrownConcreteException == null
                }
            }
        )
    }

    override fun getStaticField(fieldId: FieldId): Result<*> = delegateInstrumentation.getStaticField(fieldId)

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain,
        classfileBuffer: ByteArray
    ): ByteArray? = delegateInstrumentation.transform(
        loader, className, classBeingRedefined, protectionDomain, classfileBuffer
    )

    override fun InstrumentedProcessModel.setupAdditionalRdResponses(kryoHelper: KryoHelper, watchdog: IdleWatchdog) =
        delegateInstrumentation.run { setupAdditionalRdResponses(kryoHelper, watchdog) }

    class Factory(
        private val delegateInstrumentationFactory: UtExecutionInstrumentation.Factory<*>
    ) : UtExecutionInstrumentation.Factory<UtExecutionInstrumentation> {
        override val additionalRuntimeClasspath: Set<String>
            get() = delegateInstrumentationFactory.additionalRuntimeClasspath

        override val forceDisableSandbox: Boolean
            get() = delegateInstrumentationFactory.forceDisableSandbox

        override fun create(instrumentationContext: InstrumentationContext): UtExecutionInstrumentation =
            RemovingConstructFailsUtExecutionInstrumentation(instrumentationContext, delegateInstrumentationFactory)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Factory

            return delegateInstrumentationFactory == other.delegateInstrumentationFactory
        }

        override fun hashCode(): Int {
            return delegateInstrumentationFactory.hashCode()
        }
    }
}