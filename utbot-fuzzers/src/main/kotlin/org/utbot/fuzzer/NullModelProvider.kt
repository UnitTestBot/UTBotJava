package org.utbot.fuzzer

import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.isRefType
import java.util.function.BiConsumer

/**
 * Provides [UtNullModel] for every reference class.
 */
@Suppress("unused") // disabled until fuzzer breaks test with null/nonnull annotations
object NullModelProvider : ModelProvider {
    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
        description.parameters
            .asSequence()
            .filter { classId ->  classId.isRefType }
            .forEachIndexed { index, classId ->
                consumer.accept(index, UtNullModel(classId))
            }
    }
}