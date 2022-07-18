package org.utbot.fuzzer.baseline

import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtConcreteValue
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtValueExecution
import org.utbot.framework.plugin.api.UtMethodValueTestSet
import org.utbot.fuzzer.ObsoleteTestCaseGenerator
import org.utbot.fuzzer.baseline.generator.Generator
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

object BaselineFuzzer : ObsoleteTestCaseGenerator {
    override fun generate(method: UtMethod<*>, mockStrategy: MockStrategyApi): UtMethodValueTestSet<*> =
            Generator.generateTests(method)
}

fun successfulUtExecution(params: List<UtConcreteValue<*>>, result: Any): UtValueExecution<*> =
        UtValueExecution(params, success(result))

fun <T> failedUtExecution(params: List<UtConcreteValue<*>>, exception: Throwable): UtValueExecution<T> =
        UtValueExecution(params, failure(exception))
