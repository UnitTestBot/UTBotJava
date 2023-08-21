package org.utbot.instrumentation.instrumentation.execution.constructors

import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtCustomModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtStatementCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionData
import java.util.*

class StateBeforeAwareIdGenerator(preExistingModels: Collection<UtModel>) {
    private val seenIds = mutableSetOf<Int>()

    // there's no `IdentityHashSet`, so we use `IdentityHashMap` with dummy values
    private val seenModels = IdentityHashMap<UtModel, Unit>()

    private var nextId = 0

    init {
        collectIds(preExistingModels)
    }

    private fun collectIds(models: Collection<UtModel>) =
        models.forEach { collectIds(it) }

    private fun collectIds(model: UtModel) {
        if (model !in seenModels) {
            seenModels[model] = Unit
            (model as? UtReferenceModel)?.id?.let { seenIds.add(it) }
            when (model) {
                is UtNullModel,
                is UtPrimitiveModel,
                is UtEnumConstantModel,
                is UtClassRefModel,
                is UtVoidModel -> {}
                is UtCompositeModel -> {
                    collectIds(model.fields.values)
                    model.mocks.values.forEach { collectIds(it) }
                }
                is UtArrayModel -> {
                    collectIds(model.constModel)
                    collectIds(model.stores.values)
                }
                is UtAssembleModel -> {
                    model.origin?.let { collectIds(it) }
                    collectIds(model.instantiationCall)
                    model.modificationsChain.forEach { collectIds(it) }
                }
                is UtCustomModel -> {
                    model.origin?.let { collectIds(it) }
                    collectIds(model.dependencies)
                }
                is UtLambdaModel -> {
                    collectIds(model.capturedValues)
                }
                else -> error("Can't collect ids from $model")
            }
        }
    }

    private fun collectIds(call: UtStatementModel): Unit = when (call) {
        is UtStatementCallModel -> {
            call.instance?.let { collectIds(it) }
            collectIds(call.params)
        }
        is UtDirectSetFieldModel -> {
            collectIds(call.instance)
            collectIds(call.fieldModel)
        }
    }

    fun createId(): Int {
        while (nextId in seenIds) nextId++
        return nextId++
    }

    companion object {
        fun fromUtConcreteExecutionData(data: UtConcreteExecutionData): StateBeforeAwareIdGenerator =
            StateBeforeAwareIdGenerator(
                listOfNotNull(data.stateBefore.thisInstance) +
                        data.stateBefore.parameters +
                        data.stateBefore.statics.values +
                        data.instrumentation.flatMap {
                            when (it) {
                                is UtNewInstanceInstrumentation -> it.instances
                                is UtStaticMethodInstrumentation -> it.values
                            }
                        }
            )
    }
}