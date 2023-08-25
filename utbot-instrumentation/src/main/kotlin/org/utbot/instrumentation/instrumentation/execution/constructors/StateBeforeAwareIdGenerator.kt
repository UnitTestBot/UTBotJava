package org.utbot.instrumentation.instrumentation.execution.constructors

import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.mapper.UtModelDeepMapper.Companion.collectAllModels
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionData
import org.utbot.instrumentation.instrumentation.execution.mapModels

class StateBeforeAwareIdGenerator(allPreExistingModels: Collection<UtModel>) {
    private val seenIds = allPreExistingModels
        .filterIsInstance<UtReferenceModel>()
        .mapNotNull { it.id }
        .toMutableSet()

    private var nextId = 0

    fun createId(): Int {
        while (nextId in seenIds) nextId++
        return nextId++
    }

    companion object {
        fun fromUtConcreteExecutionData(data: UtConcreteExecutionData): StateBeforeAwareIdGenerator =
            StateBeforeAwareIdGenerator(collectAllModels { collector -> data.mapModels(collector) })
    }
}