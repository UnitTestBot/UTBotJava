package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.IdentityPreservingIdGenerator

class ArrayModelProvider(
    idGenerator: IdentityPreservingIdGenerator<Int>,
    recursion: Int = 1
) : RecursiveModelProvider(idGenerator, recursion) {
    override fun copy(idGenerator: IdentityPreservingIdGenerator<Int>, recursionDepthLeft: Int) =
        ArrayModelProvider(idGenerator, recursionDepthLeft)

    override fun generateModelConstructors(
        description: FuzzedMethodDescription,
        clazz: ClassId
    ): List<ModelConstructor> {
        if (!clazz.isArray)
            return listOf()
        val lengths = generateArrayLengths(description).sorted()
        return lengths.map { length ->
            ModelConstructor(List(length) { clazz.elementClassId!! }) { values ->
                createFuzzedArrayModel(clazz, length, values.map { it.model } )
            }
        }
    }

    private fun generateArrayLengths(description: FuzzedMethodDescription): Set<Int> {
        description.concreteValues
        val fuzzedLengths = fuzzValuesRecursively(
            types = listOf(intClassId),
            baseMethodDescription = description,
            modelProvider = ConstantsModelProvider
        )

        return fuzzedLengths
            .map { (it.single().model as UtPrimitiveModel).value as Int }
            .filter { it in 0..10 }
            .toSet()
            .plus(0)
            .plus(3)
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