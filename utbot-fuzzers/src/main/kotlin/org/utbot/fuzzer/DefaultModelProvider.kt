package org.utbot.fuzzer

import org.utbot.framework.plugin.api.*
import java.util.function.BiConsumer
import java.util.function.Function

/**
 * Simple model implementation.
 *
 * @param classToModel creates a list of [UtModel] for a particular class.
 */
@Suppress("unused")
class DefaultModelProvider(
    private val classToModel: Function<ClassId, List<UtModel>>
) : ModelProvider {
    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
        description.parametersMap.forEach { (classId, indices) ->
            val defaultModels = classToModel.apply(classId)
            defaultModels.forEach { defaultModel ->
                indices.forEach { index ->
                    consumer.accept(index, defaultModel)
                }
            }
        }
    }
}