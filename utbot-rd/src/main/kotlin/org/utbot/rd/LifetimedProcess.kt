package org.utbot.rd

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.throwIfNotAlive
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * Creates LifetimedProcess.
 * If provided parent lifetime already terminated - throws CancellationException and process is not started.
 */
fun startLifetimedProcess(cmd: List<String>, parent: Lifetime? = null): LifetimedProcess {
    parent?.throwIfNotAlive()

    return ProcessBuilder(cmd).start().toLifetimedProcess(parent)
}

/**
 * Creates LifetimedProcess from already running process.
 *
 * Process will be terminated if parent lifetime is terminated.
 */
fun Process.toLifetimedProcess(parent: Lifetime? = null): LifetimedProcess {
    return LifetimedProcessIml(this, parent)
}

/**
 * Main class goals
 * 1. lifetime terminates when process does
 * 2. process terminates when lifetime does
 * 3. optionally binding lifetime to parent scope
 */
interface LifetimedProcess {
    val lifetime: Lifetime
    val process: Process
    fun terminate()
}

inline fun <T> LifetimedProcess.use(block: (LifetimedProcess) -> T): T {
    try {
        return block(this)
    }
    finally {
        terminate()
    }
}

const val processKillTimeoutMillis = 100L
const val checkProcessAliveDelay = 100L

class LifetimedProcessIml(override val process: Process, parent: Lifetime? = null): LifetimedProcess {
    private val def: LifetimeDefinition

    override val lifetime
        get() = def.lifetime

    init {
        def = parent?.createNested() ?: LifetimeDefinition()
        def.onTermination {
            process.destroy()

            if (process.waitFor(processKillTimeoutMillis, TimeUnit.MILLISECONDS))
                process.destroyForcibly()
        }
        UtRdCoroutineScope.current.launch(def) {
            while (process.isAlive) {
                delay(checkProcessAliveDelay)
            }

            def.terminate()
        }
    }

    override fun terminate() {
        def.terminate()
    }
}