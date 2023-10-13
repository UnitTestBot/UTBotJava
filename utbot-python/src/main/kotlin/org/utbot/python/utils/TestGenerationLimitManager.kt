package org.utbot.python.utils

class TestGenerationLimitManager(
    // global settings
    var mode: LimitManagerMode,
    val until: Long,

    // local settings: one type inference iteration
    var executions: Int = 150,
    var invalidExecutions: Int = 10,
    var cacheNodeExecutions: Int = 20,
    var fakeNodeExecutions: Int = 1,
    var missedLines: Int? = null,
    val isRootManager: Boolean = false,
) {
    private val initExecution = executions
    private val initInvalidExecutions = invalidExecutions
    private val initCacheNodeExecutions = cacheNodeExecutions
    private val initFakeNodeExecutions = fakeNodeExecutions
    private val initMissedLines = missedLines

    fun restart() {
        executions = initExecution
        invalidExecutions = initInvalidExecutions
        cacheNodeExecutions = initCacheNodeExecutions
        fakeNodeExecutions = initFakeNodeExecutions
        missedLines = initMissedLines
    }

    fun addSuccessExecution() {
        executions -= 1
    }

    fun addInvalidExecution() {
        invalidExecutions -= 1
    }

    fun addFakeNodeExecutions() {
        fakeNodeExecutions -= 1
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
        return manager.invalidExecutions <= 0 || manager.executions <= 0 || manager.fakeNodeExecutions <= 0 || manager.cacheNodeExecutions <= 0
    }
}

object MaxCoverageWithTimeoutMode : LimitManagerMode {
    override fun isCancelled(manager: TestGenerationLimitManager): Boolean {
        return MaxCoverageMode.isCancelled(manager) || TimeoutMode.isCancelled(manager)
    }
}

object ExecutionWithTimoutMode : LimitManagerMode {
    override fun isCancelled(manager: TestGenerationLimitManager): Boolean {
        return ExecutionMode.isCancelled(manager) || TimeoutMode.isCancelled(manager)
    }
}
