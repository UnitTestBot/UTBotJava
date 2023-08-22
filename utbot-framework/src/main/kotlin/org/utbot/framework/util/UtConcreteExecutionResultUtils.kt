package org.utbot.framework.util

import org.utbot.framework.assemble.AssembleModelGenerator
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtModel
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import java.util.IdentityHashMap

/**
 * Tries to convert all models from [UtExecutionResult] to [UtAssembleModel] if possible.
 *
 * @return [UtConcreteExecutionResult] with converted models.
 */
fun UtConcreteExecutionResult.convertToAssemble(packageName: String): UtConcreteExecutionResult {
    val allModels = collectAllModels()

    val modelsToAssembleModels = AssembleModelGenerator(packageName).createAssembleModels(allModels)
    return updateWithAssembleModels(modelsToAssembleModels)
}

private fun UtConcreteExecutionResult.updateWithAssembleModels(
    assembledUtModels: IdentityHashMap<UtModel, UtModel>
): UtConcreteExecutionResult {
    val toAssemble: (UtModel) -> UtModel = { assembledUtModels.getOrDefault(it, it) }

    val resolvedStateBefore = stateBefore.resolveState(toAssemble)
    val resolvedStateAfter = stateAfter.resolveState(toAssemble)
    val resolvedResult = (result as? UtExecutionSuccess)?.model?.let { UtExecutionSuccess(toAssemble(it)) } ?: result

    return copy(
        stateBefore = resolvedStateBefore,
        stateAfter = resolvedStateAfter,
        result = resolvedResult,
    )
}

private fun EnvironmentModels.resolveState(toAssemble: (UtModel) -> UtModel): EnvironmentModels =
    if (this is MissingState) {
        MissingState
    } else {
        copy(
            thisInstance = thisInstance?.let { toAssemble(it) },
            parameters = parameters.map { toAssemble(it) },
            statics = statics.mapValues { toAssemble(it.value) },
        )
    }

private fun UtConcreteExecutionResult.collectAllModels(): List<UtModel> {
    val allModels = mutableListOf<UtModel>()

    allModels += stateBefore.utModels
    allModels += stateAfter.utModels
    allModels += listOfNotNull((result as? UtExecutionSuccess)?.model)

    return allModels
}

private val EnvironmentModels.utModels: List<UtModel>
    get() = listOfNotNull(thisInstance) + parameters + statics.values