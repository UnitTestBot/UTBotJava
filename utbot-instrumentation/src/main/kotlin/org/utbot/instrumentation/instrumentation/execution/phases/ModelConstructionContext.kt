package org.utbot.instrumentation.instrumentation.execution.phases

import java.security.AccessControlException
import java.util.IdentityHashMap
import org.utbot.common.withAccessibility
import org.utbot.instrumentation.instrumentation.execution.constructors.UtCompositeModelStrategy
import org.utbot.instrumentation.instrumentation.execution.constructors.UtModelConstructor
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtExplicitlyThrownException
import org.utbot.framework.plugin.api.UtImplicitlyThrownException
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtSandboxFailure
import org.utbot.framework.plugin.api.UtStreamConsumingFailure
import org.utbot.framework.plugin.api.UtTimeoutException
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jField
import org.utbot.framework.plugin.api.visible.UtStreamConsumingException
import org.utbot.instrumentation.instrumentation.et.ExplicitThrowInstruction
import org.utbot.instrumentation.instrumentation.et.TraceHandler

class ModelConstructionPhaseError(cause: Throwable) : PhaseError(
    message = "Error during model construction phase",
    cause
)

/**
 * This phase of model construction from concrete values.
 */
class ModelConstructionContext(
    private val traceHandler: TraceHandler
) : PhaseContext<ModelConstructionPhaseError> {

    override fun wrapError(error: Throwable): ModelConstructionPhaseError =
        ModelConstructionPhaseError(error)

    private lateinit var constructor: UtModelConstructor

    class ConstructorConfiguration {
        lateinit var cache: IdentityHashMap<Any, UtModel>
        lateinit var strategy: UtCompositeModelStrategy
    }

    fun configureConstructor(block: ConstructorConfiguration.() -> Unit) {
        ConstructorConfiguration().run {
            block()
            constructor = UtModelConstructor(cache, strategy)
        }
    }

    fun constructParameters(params: List<UtConcreteValue<*>>): List<UtModel> =
        params.map {
            constructor.construct(it.value, it.clazz.id)
        }

    fun constructStatics(
        stateBefore: EnvironmentModels,
        staticFields: Map<FieldId, UtConcreteValue<*>>
    ): Map<FieldId, UtModel> =
        staticFields.keys.associateWith { fieldId ->
            fieldId.jField.run {
                val computedValue = withAccessibility { get(null) }
                val knownModel = stateBefore.statics[fieldId]
                val knownValue = staticFields[fieldId]?.value
                if (knownModel != null && knownValue != null && knownValue == computedValue) {
                    knownModel
                } else {
                    constructor.construct(computedValue, fieldId.type)
                }
            }
        }

    fun convertToExecutionResult(concreteResult: Result<*>, returnClassId: ClassId): UtExecutionResult {
        val result = concreteResult.fold({
            try {
                val model = constructor.construct(it, returnClassId)
                UtExecutionSuccess(model)
            } catch (e: Exception) {
                processExceptionDuringModelConstruction(e)
            }
        }) {
            sortOutException(it)
        }
        return result
    }

    private fun sortOutException(exception: Throwable): UtExecutionFailure {
        if (exception is TimeoutException) {
            return UtTimeoutException(exception)
        }
        if (exception is AccessControlException ||
            exception is ExceptionInInitializerError && exception.exception is AccessControlException
        ) {
            return UtSandboxFailure(exception)
        }
        // there also can be other cases, when we need to wrap internal exception... I suggest adding them on demand

        val instrs = traceHandler.computeInstructionList()
        val isNested = if (instrs.isEmpty()) {
            false
        } else {
            instrs.first().callId != instrs.last().callId
        }
        return if (instrs.isNotEmpty() && instrs.last().instructionData is ExplicitThrowInstruction) {
            UtExplicitlyThrownException(exception, isNested)
        } else {
            UtImplicitlyThrownException(exception, isNested)
        }

    }

    private fun processExceptionDuringModelConstruction(e: Exception): UtExecutionResult =
        when (e) {
            is UtStreamConsumingException -> UtStreamConsumingFailure(e)
            else -> throw e
        }

}