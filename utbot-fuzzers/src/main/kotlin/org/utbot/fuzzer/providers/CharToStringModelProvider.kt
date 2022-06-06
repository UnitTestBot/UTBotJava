package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.ModelProvider
import java.util.function.BiConsumer

/**
 * Collects all char constants and creates string with them.
 */
object CharToStringModelProvider : ModelProvider {
    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
        val indices = description.parametersMap[stringClassId] ?: return
        if (indices.isNotEmpty()) {
            val string = description.concreteValues.asSequence()
                .filter { it.classId == charClassId }
                .map { it.value }
                .filterIsInstance<Char>()
                .joinToString(separator = "")
            if (string.isNotEmpty()) {
                val model = UtPrimitiveModel(string)
                indices.forEach {
                    consumer.accept(it, model)
                }
            }
        }
    }
}