package org.utbot.taint.model

import org.utbot.engine.SymbolicValue
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.taint.TaintUtil.chooseRelatedValue

/**
 * Contains symbolic values for [methodId].
 */
data class SymbolicMethodData(
    val methodId: ExecutableId,
    val base: SymbolicValue? = null,
    val args: List<SymbolicValue> = listOf(),
    val result: SymbolicValue? = null
) {
    /**
     * Returns symbolic value (base, argN or result) corresponding to the [entity].
     */
    fun choose(entity: TaintEntity): SymbolicValue? =
        entity.chooseRelatedValue(base, args, result)
}
