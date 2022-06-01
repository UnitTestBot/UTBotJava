package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.isRefType
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.ModelProvider
import java.util.function.BiConsumer

/**
 * Provides [UtNullModel] for every reference class.
 */
@Suppress("unused") // disabled until fuzzer breaks test with null/nonnull annotations
object NullModelProvider : ModelProvider {
    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
        description.parametersMap
            .asSequence()
            .filter { (classId, _) ->  classId.isRefType }
            .forEach { (classId, indices) ->
                val model = UtNullModel(classId)
                indices.forEach { consumer.accept(it, model) }
            }
    }
}