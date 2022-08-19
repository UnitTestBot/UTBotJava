package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues

class ArrayModelProvider(
    idGenerator: IdentityPreservingIdGenerator<Int>,
    recursion: Int = 1
) : RecursiveModelProvider(idGenerator, recursion) {

    private val defaultArraySize = 3

    private val limitRecursivelyFuzzed =
        when(recursion) {
            1 -> Int.MAX_VALUE
            else -> 3
        }

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap
            .asSequence()
            .filter { (classId, _) -> classId.isArray }
            .forEach { (arrayClassId, indices) ->

                // Fuzz small arrays with interesting elements
                yieldAllValues(indices, generateArrayRecursively(arrayClassId, description, defaultArraySize))

                // Fuzz arrays with interesting lengths and default-valued elements
                val lengths = generateArrayLengths(description)
                yieldAllValues(indices, lengths.asSequence().map { length ->
                    createFuzzedArrayModel(arrayClassId, length, null)
                })
            }
    }

    private fun generateArrayLengths(description: FuzzedMethodDescription): Set<Int> {
        val fuzzedLengths = fuzzValuesRecursively(
            types = listOf(intClassId),
            baseMethodDescription = description,
            modelProvider = ConstantsModelProvider,
            generatedValuesName = "array length"
        )

        return fuzzedLengths
            .map { (it.single().model as UtPrimitiveModel).value as Int }
            .filter { it in 0..10 }
            .toSet()
            .plus(0)
    }

    private fun generateArrayRecursively(arrayClassId: ClassId, description: FuzzedMethodDescription, length: Int): Sequence<FuzzedValue> {
        val elementClassId = arrayClassId.elementClassId ?: error("expected ClassId for array but got ${arrayClassId.name}")
        val fuzzedArrayElements = fuzzValuesRecursively(
            types = List(length) { elementClassId },
            baseMethodDescription = description,
            modelProvider = generateRecursiveProvider(),
            generatedValuesName = "elements of array"
        )

        return fuzzedArrayElements
            .take(limitRecursivelyFuzzed)
            .map { elements ->
                createFuzzedArrayModel(arrayClassId, length, elements.map { it.model })
            }
    }

    private fun createFuzzedArrayModel(arrayClassId: ClassId, length: Int, values: List<UtModel>?) =
        UtArrayModel(
            idGenerator.createId(),
            arrayClassId,
            length,
            arrayClassId.elementClassId!!.defaultValueModel(),
            values?.withIndex()?.associate { it.index to it.value }?.toMutableMap() ?: mutableMapOf()
        ).fuzzed {
            this.summary = "%var% = ${arrayClassId.elementClassId!!.simpleName}[$length]"
        }
}