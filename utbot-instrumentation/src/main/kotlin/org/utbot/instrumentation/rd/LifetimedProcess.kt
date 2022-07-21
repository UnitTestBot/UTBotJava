package org.utbot.instrumentation.rd

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlinx.coroutines.delay
import org.utbot.common.pid

fun startLifetimedProcess(cmd: List<String>, parent: Lifetime? = null): LifetimedProcess {
    return ProcessBuilder(cmd).start().toLifetimedProcess(parent)
}

fun Process.toLifetimedProcess(parent: Lifetime? = null): LifetimedProcess {
    return LifetimedProcessIml(this, parent)
}

/**
 * Main class goals
 * 1. provide lifetime that terminates when process does
 * 2. provide process that terminates when lifetime does
 * 3. optionally binding lifetime to parent scope
 */
interface LifetimedProcess {
    val lifetime: Lifetime
    val pid: Long
    val process: Process
    fun terminate()
}

inline fun <T> LifetimedProcess.use(block: () -> T): T {
    try {
        return block()
    }
    finally {
        terminate()
    }
}

class LifetimedProcessIml(override val process: Process, parent: Lifetime? = null): LifetimedProcess {
    private val def: LifetimeDefinition

    override val lifetime
        get() = def.lifetime

    override val pid
        get() = process.pid

    init {
        def = parent?.createNested() ?: LifetimeDefinition()
        def.onTermination {
            process.destroy() // todo this might not kill process instantly
        }
        UtRdCoroutineScope.current.launch(def) {
            while (process.isAlive) {
                delay(100) // todo tune
            }

            def.terminate()
        }
    }

    override fun terminate() {
        def.terminate()
    }
}