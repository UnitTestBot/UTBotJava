package org.utbot.instrumentation.instrumentation.execution.phases

import com.jetbrains.rd.util.getLogger
import org.utbot.common.measureTime
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.rd.loggers.debug

private val logger = getLogger<ExecutionPhase>()

abstract class ExecutionPhaseException(override val message: String) : Exception()

// Execution will be stopped, exception will be thrown in engine process
class ExecutionPhaseError(phase: String, override val cause: Throwable) : ExecutionPhaseException(phase)

// Execution will be stopped, but considered successful, result will be returned
class ExecutionPhaseStop(phase: String, val result: UtConcreteExecutionResult) : ExecutionPhaseException(phase)

interface ExecutionPhase {
    fun wrapError(e: Throwable): ExecutionPhaseException
}

fun <T : ExecutionPhase, R> T.start(block: T.() -> R): R =
    try {
        logger.debug().measureTime({ this.javaClass.simpleName } ) {
            this.block()
        }
    } catch (e: ExecutionPhaseStop) {
        throw e
    } catch (e: Throwable) {
        throw this.wrapError(e)
    }
