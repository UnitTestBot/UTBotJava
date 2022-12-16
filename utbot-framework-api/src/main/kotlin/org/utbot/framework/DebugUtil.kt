package org.utbot.framework

import java.lang.management.ManagementFactory
import java.lang.management.RuntimeMXBean

fun isProcessDebugged(): Boolean {
    val runtimeMXBean: RuntimeMXBean = ManagementFactory.getRuntimeMXBean()
    val jvmArguments: String = runtimeMXBean.inputArguments.toString()

    return jvmArguments.contains("-agentlib:jdwp")
}

fun isAnyUtBotProcessDebugged(): Boolean {
    return isProcessDebugged() || UtSettings.runEngineProcessWithDebug || UtSettings.runInstrumentedProcessWithDebug
}