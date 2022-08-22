package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue

/**
 * Collects all char constants and creates string with them.
 */
object CharToStringModelProvider : ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        val indices = description.parametersMap[stringClassId] ?: return@sequence
        if (indices.isNotEmpty()) {
            val string = description.concreteValues.asSequence()
                .filter { it.classId == charClassId }
                .map { it.value }
                .filterIsInstance<Char>()
                .joinToString(separator = "")
            if (string.isNotEmpty()) {
                sequenceOf(string.reversed(), string).forEach { str ->
                    val model = UtPrimitiveModel(str).fuzzed()
                    indices.forEach {
                        yieldValue(it, model)
                    }
                }
            }
        }
    }
}