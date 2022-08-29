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
    recursionDepthLeft: Int = 1
) : RecursiveModelProvider(idGenerator, recursionDepthLeft) {
    override fun createNewInstance(parentProvider: RecursiveModelProvider, newTotalLimit: Int): RecursiveModelProvider =
        ArrayModelProvider(parentProvider.idGenerator, parentProvider.recursionDepthLeft - 1)
            .copySettingsFrom(parentProvider)
            .apply {
                totalLimit = newTotalLimit
                if (parentProvider is ArrayModelProvider)
                    branchingLimit = 1      // This improves performance but also forbids generating "jagged" arrays
            }

    override fun generateModelConstructors(
        description: FuzzedMethodDescription,
        classId: ClassId
    ): List<ModelConstructor> {
        if (!classId.isArray)
            return listOf()
        val lengths = generateArrayLengths(description)
        return lengths.map { length ->
            ModelConstructor(List(length) { classId.elementClassId!! }) { values ->
                createFuzzedArrayModel(classId, length, values.map { it.model } )
            }
        }
    }

    private fun generateArrayLengths(description: FuzzedMethodDescription): List<Int> {
        val fuzzedLengths = fuzzValuesRecursively(
            types = listOf(intClassId),
            baseMethodDescription = description,
            modelProvider = ConstantsModelProvider
        )

        // Firstly we will use most "interesting" default sizes, then we will use small sizes obtained from src
        return listOf(3, 0) + fuzzedLengths
            .map { (it.single().model as UtPrimitiveModel).value as Int }
            .filter { it in 1..10 && it != 3 }
            .toSortedSet()
            .toList()
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