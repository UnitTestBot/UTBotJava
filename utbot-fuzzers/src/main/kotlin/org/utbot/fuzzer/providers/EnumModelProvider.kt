package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isSubtypeOf
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.consumeAll
import java.util.function.BiConsumer

object EnumModelProvider : ModelProvider {
    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
        description.parametersMap
            .asSequence()
            .filter { (classId, _) -> classId.isSubtypeOf(Enum::class.java.id) }
            .forEach { (classId, indices) ->
                consumer.consumeAll(indices, classId.jClass.enumConstants.filterIsInstance<Enum<*>>().map {
                    UtEnumConstantModel(classId, it)
                })
            }
    }
}