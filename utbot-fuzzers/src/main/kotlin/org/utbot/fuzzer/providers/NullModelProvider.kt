package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.isRefType
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue

/**
 * Provides [UtNullModel] for every reference class.
 */
@Suppress("unused") // disabled until fuzzer breaks test with null/nonnull annotations
object NullModelProvider : ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap
            .asSequence()
            .filter { (classId, _) ->  classId.isRefType }
            .forEach { (classId, indices) ->
                val model = UtNullModel(classId)
                indices.forEach {
                    yieldValue(it, model.fuzzed { this.summary = "%var% = null" }) }
            }
    }
}