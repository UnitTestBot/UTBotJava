package org.utbot.framework.util

import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtCustomModel
import org.utbot.framework.plugin.api.UtDirectGetFieldModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.UtVoidModel

fun EnvironmentModels.calculateSize(): Int {
    val thisInstanceSize = thisInstance?.calculateSize() ?: 0
    val parametersSize = parameters.sumOf { it.calculateSize() }
    val staticsSize = statics.values.sumOf { it.calculateSize() }

    return thisInstanceSize + parametersSize + staticsSize
}

/**
 * We assume that "size" for "common" models is 1, 0 for [UtVoidModel] (as they do not return anything) and
 * [UtPrimitiveModel] and 3 for [UtNullModel] (we use them as literals in codegen), summarising for
 * all statements for [UtAssembleModel] and summarising for all fields and mocks for [UtCompositeModel].
 *
 * As [UtCompositeModel] could be recursive, we need to store it in [used]. Moreover, if we already
 * calculate size for [this], it means that we will use already created variable by this model and do not
 * need to create it again, so size should be equal to 0.
 */
private fun UtModel.calculateSize(used: MutableSet<UtReferenceModel> = mutableSetOf()): Int {
    if (this in used) return 0

    if (this is UtReferenceModel)
        used += this

    return when (this) {
        // `null` is assigned positive size to encourage use of non-null values
        is UtNullModel -> 3
        is UtPrimitiveModel, UtVoidModel -> 0
        is UtClassRefModel, is UtEnumConstantModel, is UtArrayModel, is UtCustomModel -> 1
        is UtAssembleModel -> {
            1 + instantiationCall.calculateSize(used) + modificationsChain.sumOf { it.calculateSize(used) }
        }
        is UtCompositeModel -> 1 + (fields.values + mocks.values.flatten()).sumOf { it.calculateSize(used) }
        is UtLambdaModel -> 1 + capturedValues.sumOf { it.calculateSize(used) }
        // PythonModel, JsUtModel, UtSpringContextModel may be here
        else -> 0
    }
}

private fun UtStatementModel.calculateSize(used: MutableSet<UtReferenceModel> = mutableSetOf()): Int =
    when (this) {
        is UtExecutableCallModel -> 1 + params.sumOf { it.calculateSize(used) } + (instance?.calculateSize(used) ?: 0)
        is UtDirectSetFieldModel -> 1 + fieldModel.calculateSize(used) + instance.calculateSize(used)

        // -2 is added to encourage use of non-hardcoded values (including compensation for one extra `UtAssembleModel`)
        is UtDirectGetFieldModel -> (-2 + instance.calculateSize(used)).coerceAtLeast(0)
    }
