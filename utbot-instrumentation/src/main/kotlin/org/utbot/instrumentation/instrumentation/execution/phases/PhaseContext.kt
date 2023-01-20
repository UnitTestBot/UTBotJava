package org.utbot.instrumentation.instrumentation.execution.phases

import com.jetbrains.rd.util.debug
import com.jetbrains.rd.util.getLogger
import org.utbot.instrumentation.instrumentation.execution.UtConcreteExecutionResult
import org.utbot.rd.logMeasure

val logger = getLogger("Executionphase")

abstract class ExecutionPhaseException(override val message: String): Exception()

// InstrumentationProcessError
class ExecutionPhaseError(phase: String, override val cause: Throwable) : ExecutionPhaseException(phase)

// Ok
class ExecutionPhaseStop(phase: String, val result: UtConcreteExecutionResult): ExecutionPhaseException(phase   )

interface ExecutionPhase {
    fun wrapError(e: Throwable): ExecutionPhaseException
}

fun <T: ExecutionPhase, R> T.start(block: T.() -> R): R =
    try {
        logger.logMeasure(this.javaClass.simpleName) {
            this.block()
        }
    }
    catch (e: ExecutionPhaseStop) {
        throw e
    }
    catch (e: Throwable) {
        throw this.wrapError(e)
    }
