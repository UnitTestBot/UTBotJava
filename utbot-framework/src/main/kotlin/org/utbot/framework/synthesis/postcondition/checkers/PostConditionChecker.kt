package org.utbot.framework.synthesis.postcondition.checkers

import org.utbot.framework.plugin.api.UtModel

internal fun interface PostConditionChecker {
    fun checkPostCondition(actualModel: UtModel): Boolean
}