package org.utbot.framework.util

import org.utbot.framework.assemble.AssembleModelGenerator
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtModel
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import java.util.IdentityHashMap

private fun UtConcreteExecutionResult.updateWithAssembleModels(
    assembledUtModels: IdentityHashMap<UtModel, UtModel>
): UtConcreteExecutionResult {
    val toAssemble: (UtModel) -> UtModel = { assembledUtModels.getOrDefault(it, it) }

    val resolvedStateAfter = if (stateAfter is MissingState) MissingState else EnvironmentModels(
        stateAfter.thisInstance?.let { toAssemble(it) },
        stateAfter.parameters.map { toAssemble(it) },
        stateAfter.statics.mapValues { toAssemble(it.value) }
    )
    val resolvedResult =
        (result as? UtExecutionSuccess)?.model?.let { UtExecutionSuccess(toAssemble(it)) } ?: result

    return UtConcreteExecutionResult(
        resolvedStateAfter,
        resolvedResult,
        coverage
    )
}

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

private fun UtConcreteExecutionResult.collectAllModels(): List<UtModel> {
    val allModels = listOfNotNull(stateAfter.thisInstance).toMutableList()
    allModels += stateAfter.parameters
    allModels += stateAfter.statics.values
    allModels += listOfNotNull((result as? UtExecutionSuccess)?.model)
    return allModels
}