package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues
import org.utbot.fuzzer.defaultModelProviders
import org.utbot.fuzzer.fuzz

class ArrayModelProvider(
    private val idGenerator: IdentityPreservingIdGenerator<Int>
) : ModelProvider {

    private val defaultArraySize: Int = 3

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap
            .asSequence()
            .filter { (classId, _) -> classId.isArray }
            .forEach { (arrayClassId, indices) ->
                val lengths = generateArrayLengths(description)

                // Fuzz small arrays with interesting elements
                yieldAllValues(indices, generateArrayRecursively(arrayClassId, description, defaultArraySize))

                // Fuzz arrays with interesting lengths and default-valued elements
                yieldAllValues(indices, lengths.asSequence().map { length ->
                        UtArrayModel(
                            id = idGenerator.createId(),
                            arrayClassId,
                            length = length,
                            arrayClassId.elementClassId!!.defaultValueModel(),
                            mutableMapOf()
                        ).fuzzed {
                            this.summary = "%var% = ${arrayClassId.elementClassId!!.simpleName}[$length]"
                        }
                })
            }
    }

    private fun generateArrayLengths(description: FuzzedMethodDescription): Set<Int> {
        val syntheticArrayLengthMethodDescription = FuzzedMethodDescription(
            "<syntheticArrayLength>",
            voidClassId,
            listOf(intClassId),
            description.concreteValues
        ).apply {
            packageName = description.packageName
        }

        return fuzz(syntheticArrayLengthMethodDescription, ConstantsModelProvider)
            .map { (it.single().model as UtPrimitiveModel).value as Int }
            .filter { it in 0..10 }
            .toSet()
            .plus(0)
    }

    private fun generateArrayRecursively(arrayClassId: ClassId, description: FuzzedMethodDescription, length: Int): Sequence<FuzzedValue> {
        val elementClassId = arrayClassId.elementClassId ?: error("expected ClassId for array but got ${arrayClassId.name}")
        val syntheticArrayElementSetterMethodDescription = FuzzedMethodDescription(
            "${arrayClassId.simpleName}OfLength$length<syntheticArrayElementSetter>",
            voidClassId,
            List(length) { elementClassId },
            description.concreteValues
        ).apply {
            packageName = description.packageName
        }
        return fuzz(syntheticArrayElementSetterMethodDescription, defaultModelProviders(idGenerator)).map {
            FuzzedValue(
                UtArrayModel(
                    idGenerator.createId(),
                    arrayClassId,
                    length,
                    elementClassId.defaultValueModel(),
                    it.withIndex().associate { it.index to it.value.model }.toMutableMap()
                )
            )
        }
    }
}