package org.utbot.testing

import org.utbot.framework.plugin.api.UtExecutionFailure
import org.utbot.framework.plugin.api.UtExecutionResult
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtModel

fun UtExecutionResult.getOrThrow(): UtModel = when (this) {
    is UtExecutionSuccess -> model
    is UtExecutionFailure -> throw exception
}
