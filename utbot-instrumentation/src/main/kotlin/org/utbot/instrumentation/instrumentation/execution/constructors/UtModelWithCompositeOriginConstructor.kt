package org.utbot.instrumentation.instrumentation.execution.constructors

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtModelWithCompositeOrigin

/**
 * Responsible for constructing [UtModelWithCompositeOrigin]s of some specific type, that are more human-readable
 * when rendered by the code generation compared to [UtCompositeModel]s.
 */
interface UtModelWithCompositeOriginConstructor {

    /**
     * @param internalConstructor constructor to use for constructing child models
     * (e.g. when [value] is a list, [internalConstructor] is used for constructing list elements)
     * @param value object to construct model for
     * @param valueClassId [ClassId] to use for constructed model
     * @param saveToCache function that should be called on the returned model right after constructing it,
     * but before adding any modifications, so [internalConstructor] doesn't have to reconstruct it for every modification
     * and recursive values (e.g. list containing itself) are constructed correctly
     */
    fun constructModelWithCompositeOrigin(
        internalConstructor: UtModelConstructorInterface,
        value: Any,
        valueClassId: ClassId,
        id: Int?,
        saveToCache: (UtModel) -> Unit
    ): UtModelWithCompositeOrigin
}