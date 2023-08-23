package org.utbot.instrumentation.instrumentation.execution.constructors

import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import org.utbot.framework.plugin.api.mapper.collectNestedModels
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionData

class StateBeforeAwareIdGenerator(preExistingModels: Collection<UtModel>) {
    private val seenIds = collectNestedModels(preExistingModels)
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