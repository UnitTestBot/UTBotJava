package org.utbot.fuzzer

import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.stringClassId
import java.util.function.BiConsumer

/**
 * Traverses through method constants and creates appropriate models for them.
 */
object ConstantsModelProvider : ModelProvider {

    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
        description.concreteValues
            .asSequence()
            .filter { (classId, _) -> classId.isPrimitive || classId == stringClassId }
            .forEach { (_, value) ->
                val model = UtPrimitiveModel(value)
                description.parametersMap.getOrElse(model.classId) { emptyList() }.forEach { index ->
                    consumer.accept(index, model)
                }
        }
    }
}