package org.utbot.rd

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * Creates LifetimedProcess.
 * If provided lifetime already terminated - throws CancellationException and process is not started.
 */
fun startLifetimedProcess(cmd: List<String>, lifetime: Lifetime? = null): LifetimedProcess {
    lifetime?.throwIfNotAlive()

    return ProcessBuilder(cmd).start().toLifetimedProcess(lifetime)
}

/**
 * Creates LifetimedProcess from already running process.
 *
 * Process will be terminated if provided lifetime is terminated.
 */
fun Process.toLifetimedProcess(lifetime: Lifetime? = null): LifetimedProcess {
    return LifetimedProcessIml(this, lifetime)
}

/**
 * Main class goals
 * 1. if process terminates - lifetime terminates
 * 2. if lifetime terminates - process terminates
 */
interface LifetimedProcess {
    val lifetime: Lifetime
    val process: Process
    fun terminate()
}

inline fun <T, R: LifetimedProcess> R.use(block: (R) -> T): T {
    try {
        return block(this)
    }
    finally {
        terminate()
    }
}

inline fun <T, R: LifetimedProcess> R.terminateOnException(block: (R) -> T): T {
    try {
        return block(this)
    }
    catch(e: Throwable) {
        terminate()
        throw e
    }
}

const val processKillTimeoutMillis = 100L
const val checkProcessAliveDelay = 100L

class LifetimedProcessIml(override val process: Process, lifetime: Lifetime? = null): LifetimedProcess {
    private val ldef: LifetimeDefinition

    override val lifetime
        get() = ldef.lifetime

    init {
        ldef = lifetime?.createNested() ?: LifetimeDefinition()
        ldef.onTermination {
            process.destroy()

            if (process.waitFor(processKillTimeoutMillis, TimeUnit.MILLISECONDS))
                process.destroyForcibly()
        }
        UtRdCoroutineScope.current.launch(ldef) {
            while (process.isAlive) {
                delay(checkProcessAliveDelay)
            }

            ldef.terminate()
        }
    }

    override fun terminate() {
        ldef.terminate()
    }
}