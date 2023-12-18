package org.utbot.usvm.machine

import mu.KotlinLogging
import org.jacodb.api.JcMethod
import org.usvm.api.targets.JcTarget
import org.usvm.machine.JcMachine
import org.usvm.machine.state.JcState
import org.usvm.statistics.collectors.StatesCollector
import org.utbot.common.ThreadBasedExecutor
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.framework.plugin.api.util.withUtContext

private val logger = KotlinLogging.logger {}

fun JcMachine.analyzeAsync(
    forceTerminationTimeout: Long,
    methods: List<JcMethod>,
    targets: List<JcTarget>,
    callback: (JcState) -> Unit
) {
    val utContext = utContext
    // TODO usvm-sbft: sometimes `machine.analyze` or `executor.execute` hangs forever,
    //  completely ignoring timeout specified for it, so additional hard time out is enforced here.
    //  Hard timeout seems to be working ok so far, however it may leave machine or executor in an inconsistent state.
    //  Also, `machine` or `executor` may catch `ThreadDeath` and still continue working (that is in fact what happens,
    //  but throwing `ThreadDeath` every 500 ms seems to eventually work).
    ThreadBasedExecutor.threadLocal.invokeWithTimeout(forceTerminationTimeout, threadDeathThrowPeriodMillis = 500) {
        withUtContext(utContext) {
            analyze(
                methods = methods,
                statesCollector = object : StatesCollector<JcState> {
                    override var count: Int = 0
                        private set

                    override fun addState(state: JcState) {
                        count++
                        callback(state)
                    }
                },
                targets = targets
            )
        }
    }?.onFailure { e ->
        logger.error(e) { "analyzeAsync failed" }
    } ?: logger.error { "analyzeAsync time exceeded hard time out" }
}