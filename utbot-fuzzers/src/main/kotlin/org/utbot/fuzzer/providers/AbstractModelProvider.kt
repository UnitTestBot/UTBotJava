package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue
import org.utbot.fuzzer.types.Type

/**
 * Simple model implementation.
 */
@Suppress("unused")
abstract class AbstractModelProvider: ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence{
        description.parametersMap.forEach { (type, indices) ->
            toModel(type)?.let { defaultModel ->
                indices.forEach { index ->
                    yieldValue(index, defaultModel.fuzzed())
                }
            }
        }
    }

    abstract fun toModel(type: Type): UtModel?
}