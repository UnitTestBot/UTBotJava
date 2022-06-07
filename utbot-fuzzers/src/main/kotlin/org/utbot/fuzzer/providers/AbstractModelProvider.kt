package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.*
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.ModelProvider
import java.util.function.BiConsumer

/**
 * Simple model implementation.
 */
@Suppress("unused")
abstract class AbstractModelProvider: ModelProvider {
    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
        description.parametersMap.forEach { (classId, indices) ->
            toModel(classId)?.let { defaultModel ->
                indices.forEach { index ->
                    consumer.accept(index, defaultModel)
                }
            }
        }
    }

    abstract fun toModel(classId: ClassId): UtModel?
}