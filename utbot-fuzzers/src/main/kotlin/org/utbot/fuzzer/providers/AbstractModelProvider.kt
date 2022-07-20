package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue

/**
 * Simple model implementation.
 */
@Suppress("unused")
abstract class AbstractModelProvider: ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence{
        description.parametersMap.forEach { (classId, indices) ->
            toModel(classId)?.let { defaultModel ->
                indices.forEach { index ->
                    yieldValue(index, defaultModel.fuzzed())
                }
            }
        }
    }

    abstract fun toModel(classId: ClassId): UtModel?
}