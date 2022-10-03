package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.fuzzNumbers

class ArrayModelProvider(
    idGenerator: IdentityPreservingIdGenerator<Int>,
    recursionDepthLeft: Int = 2
) : RecursiveModelProvider(idGenerator, recursionDepthLeft) {

    override fun newInstance(parentProvider: RecursiveModelProvider, constructor: ModelConstructor): RecursiveModelProvider {
        val provider = ArrayModelProvider(parentProvider.idGenerator, parentProvider.recursionDepthLeft - 1)
        provider.copySettings(parentProvider)
        provider.totalLimit = minOf(parentProvider.totalLimit, constructor.limit)
        return provider
    }

    override fun generateModelConstructors(
        description: FuzzedMethodDescription,
        parameterIndex: Int,
        classId: ClassId,
    ): Sequence<ModelConstructor> = sequence {
        if (!classId.isArray) return@sequence
        val lengths = fuzzNumbers(description.concreteValues, 0, 3) { it in 1..10 }.toList()
        lengths.forEach { length ->
            yield(ModelConstructor(listOf(FuzzedType(classId.elementClassId!!)), repeat = length) { values ->
                createFuzzedArrayModel(classId, length, values.map { it.model } )
            }.apply {
                limit = (totalLimit / lengths.size).coerceAtLeast(1)
            })
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