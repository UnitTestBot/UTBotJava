package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.consumeAll
import java.util.function.BiConsumer
import java.util.function.IntSupplier

class ArrayModelProvider(
    private val idGenerator: IntSupplier
) : ModelProvider {
    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
        description.parametersMap
            .asSequence()
            .filter { (classId, _) -> classId.isArray }
            .forEach { (arrayClassId, indices) ->
                consumer.consumeAll(indices, listOf(0, 10).map { arraySize ->
                    UtArrayModel(
                        id = idGenerator.asInt,
                        arrayClassId,
                        length = arraySize,
                        arrayClassId.elementClassId!!.defaultValueModel(),
                        mutableMapOf()
                    )
                })
            }
    }
}