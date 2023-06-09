package org.utbot.taint.model

import org.utbot.engine.SymbolicValue
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.taint.TaintUtil.chooseRelatedValue

/**
 * Contains symbolic values for [methodId].
 */
data class SymbolicMethodData(
    val methodId: ExecutableId,
    val base: SymbolicValue?,
    val args: List<SymbolicValue>,
    val result: SymbolicValue?
) {
    /**
     * Returns symbolic value (base, argN or result) corresponding to the [entity].
     */
    fun choose(entity: TaintEntity): SymbolicValue? =
        entity.chooseRelatedValue(base, args, result)

    companion object {
        fun constructInvalid(methodId: ExecutableId): SymbolicMethodData =
            SymbolicMethodData(methodId, base = null, args = listOf(), result = null)
    }
}
