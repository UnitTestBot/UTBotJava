package org.utbot.framework.plugin.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import org.utbot.engine.UtBotSymbolicEngine
import org.utbot.framework.UtSettings

/**
 * Constructs [TestFlow] for customization and creates flow producer.
 */
fun testFlow(block: TestFlow.() -> Unit): UtBotSymbolicEngine.() -> Flow<UtResult> =  { TestFlow(block).build(this) }

/**
 * Creates default flow that uses [UtSettings] for customization.
 */
fun defaultTestFlow(timeout: Long) = testFlow {
    isSymbolicEngineEnabled = true
    generationTimeout = timeout
    isFuzzingEnabled = UtSettings.useFuzzing
    if (generationTimeout > 0) {
        fuzzingValue = UtSettings.fuzzingTimeoutInMillis.coerceIn(0, generationTimeout) / generationTimeout.toDouble()
    }
}

/**
 * Creates default flow that uses [UtSettings] for customization.
 */
fun defaultTestFlow(engine: UtBotSymbolicEngine, timeout: Long) = defaultTestFlow(timeout).invoke(engine)

/**
 * TestFlow helps construct flows with different settings but with common sequence of test generation process.
 *
 * Use [testFlow] to set a custom test flow or [defaultTestFlow] to create flow based on [UtSettings].
 */
class TestFlow internal constructor(block: TestFlow.() -> Unit) {
    var generationTimeout = 0L
        set(value) {
            field = maxOf(0, value)
        }
    var isSymbolicEngineEnabled = true
    var isFuzzingEnabled = false
    var fuzzingValue: Double = 0.1
        set(value) {
            field = value.coerceIn(0.0, 1.0)
        }

    init {
        apply(block)
    }

    /*
        Constructs default flow that is having the following steps at the moment:
        1. If fuzzer is enabled then run it before symbolic execution for [fuzzingValue] * [generationTimeout] ms.
        2. Run symbolic execution for the rest time.
        3. If both (fuzzer and symbolic execution) are off then do nothing.
     */
    fun build(engine: UtBotSymbolicEngine): Flow<UtResult>  {
        return when {
            generationTimeout == 0L -> emptyFlow()
            isFuzzingEnabled -> {
                when (val value = if (isSymbolicEngineEnabled) (fuzzingValue * generationTimeout).toLong() else generationTimeout) {
                    0L -> engine.traverse()
                    generationTimeout -> engine.fuzzing(System.currentTimeMillis() + value)
                    else -> flowOf(
                        engine.fuzzing(System.currentTimeMillis() + value),
                        engine.traverse()
                    ).flattenConcat()
                }
            }
            isSymbolicEngineEnabled -> engine.traverse()
            else -> emptyFlow()
        }
    }
}