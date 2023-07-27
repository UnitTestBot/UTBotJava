package org.utbot.instrumentation.instrumentation.execution.constructors

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel

/**
 * Responsible for constructing [UtModel]s of some specific type, that are more human-readable
 * when rendered by the code generation compared to [UtCompositeModel]s.
 */
interface UtCustomModelConstructor {
    fun constructCustomModel(
        internalConstructor: UtModelConstructorInterface,
        value: Any,
        valueClassId: ClassId,
        id: Int?,
        saveToCache: (UtModel) -> Unit
    ): UtModel
}