package org.utbot.framework.plugin.api.mapper

import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.UtDirectGetFieldModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtStatementCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import org.utbot.framework.plugin.api.isSuccess

inline fun <reified T : UtModel> T.mapPreservingType(mapper: UtModelMapper): T =
    mapper.map(this, T::class.java)

fun UtModel.map(mapper: UtModelMapper) = mapPreservingType<UtModel>(mapper)

fun List<UtModel>.mapModels(mapper: UtModelMapper): List<UtModel> =
    map { model -> model.map(mapper) }

fun <K> Map<K, UtModel>.mapModelValues(mapper: UtModelMapper): Map<K, UtModel> =
    mapValues { (_, model) -> model.map(mapper) }

fun UtStatementModel.mapModels(mapper: UtModelMapper): UtStatementModel =
    when(this) {
        is UtStatementCallModel -> mapModels(mapper)
        is UtDirectSetFieldModel -> UtDirectSetFieldModel(
            instance = instance.mapPreservingType<UtReferenceModel>(mapper),
            fieldId = fieldId,
            fieldModel = fieldModel.map(mapper)
        )
    }

fun UtStatementCallModel.mapModels(mapper: UtModelMapper): UtStatementCallModel =
    when(this) {
        is UtDirectGetFieldModel -> UtDirectGetFieldModel(
            instance = instance.mapPreservingType<UtReferenceModel>(mapper),
            fieldAccess = fieldAccess,
        )
        is UtExecutableCallModel -> UtExecutableCallModel(
            instance = instance?.mapPreservingType<UtReferenceModel>(mapper),
            executable = executable,
            params = params.mapModels(mapper)
        )
    }

fun EnvironmentModels.mapModels(mapper: UtModelMapper) = EnvironmentModels(
    thisInstance = thisInstance?.map(mapper),
    statics = statics.mapModelValues(mapper),
    parameters = parameters.mapModels(mapper),
    executableToCall = executableToCall,
)

fun UtExecutionResult.mapModelIfExists(mapper: UtModelMapper) = if (this.isSuccess) {
    val successResult = this as UtExecutionSuccess
    UtExecutionSuccess(successResult.model.map(mapper))
} else {
    this
}


fun UtInstrumentation.mapModels(mapper: UtModelMapper) = when (this) {
    is UtNewInstanceInstrumentation -> copy(instances = instances.mapModels(mapper))
    is UtStaticMethodInstrumentation -> copy(values = values.mapModels(mapper))
}

fun UtExecution.mapStateBeforeModels(mapper: UtModelMapper) = copy(
    stateBefore = stateBefore.mapModels(mapper)
)
