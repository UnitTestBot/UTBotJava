package org.utbot.python.utils

import kotlin.math.min

class TestGenerationLimitManager(
    // global settings
    var mode: LimitManagerMode,
    val until: Long,

    // local settings: one type inference iteration
    var executions: Int = 150,
    var invalidExecutions: Int = 10,
    var additionalExecutions: Int = 5,
    var missedLines: Int? = null,
    var withRepeatedExecutions: Int = 2000
) {
    private val initExecution = executions
    private val initInvalidExecutions = invalidExecutions
    private val initAdditionalExecutions = additionalExecutions
    private val initMissedLines = missedLines
    private val initWithRepeatedExecutions = withRepeatedExecutions

    fun restart() {
        executions = initExecution
        invalidExecutions = initInvalidExecutions
        additionalExecutions = initAdditionalExecutions
        missedLines = initMissedLines
        withRepeatedExecutions = initWithRepeatedExecutions
    }

    fun addSuccessExecution() {
        executions -= 1
        withRepeatedExecutions -= 1
    }

    fun addInvalidExecution() {
        invalidExecutions -= 1
        withRepeatedExecutions -= 1
    }

    fun addRepeatedExecution() {
        withRepeatedExecutions -= 1
    }

    fun isCancelled(): Boolean {
        return mode.isCancelled(this)
    }
}

interface LimitManagerMode {
    fun isCancelled(manager: TestGenerationLimitManager): Boolean
}

object MaxCoverageMode : LimitManagerMode {
    override fun isCancelled(manager: TestGenerationLimitManager): Boolean {
        return manager.missedLines?.equals(0) == true
    }
}

object TimeoutMode : LimitManagerMode {
    override fun isCancelled(manager: TestGenerationLimitManager): Boolean {
        return System.currentTimeMillis() >= manager.until
    }
}

object ExecutionMode : LimitManagerMode {
    override fun isCancelled(manager: TestGenerationLimitManager): Boolean {
        if (manager.invalidExecutions <= 0 || manager.executions <= 0 || manager.withRepeatedExecutions <= 0) {
            return (manager.withRepeatedExecutions <= 0) ||
                    min(manager.invalidExecutions, 0) + min(manager.executions, 0) + manager.additionalExecutions <= 0
        }
        return false
    }
}

object MaxCoverageWithTimeoutMode : LimitManagerMode {
    override fun isCancelled(manager: TestGenerationLimitManager): Boolean {
        return MaxCoverageMode.isCancelled(manager) || TimeoutMode.isCancelled(manager)
    }
}

object ExecutionWithTimeoutMode : LimitManagerMode {
    override fun isCancelled(manager: TestGenerationLimitManager): Boolean {
        return ExecutionMode.isCancelled(manager) || TimeoutMode.isCancelled(manager)
    }
}
