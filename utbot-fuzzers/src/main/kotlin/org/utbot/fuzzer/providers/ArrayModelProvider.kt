package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.fuzzer.*
import org.utbot.fuzzer.types.JavaArray
import org.utbot.fuzzer.types.Type
import org.utbot.fuzzer.types.WithClassId

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
        type: Type,
    ): Sequence<ModelConstructor> = sequence {
        if (type !is JavaArray) return@sequence
        val lengths = fuzzNumbers(description.concreteValues, 0, 3) { it in 1..10 }.toList()
        lengths.forEach { length ->
            yield(ModelConstructor(listOf(FuzzedType(type.elementType)), repeat = length) { values ->
                createFuzzedArrayModel(type, length, values.map { it.model } )
            }.apply {
                limit = (totalLimit / lengths.size).coerceAtLeast(1)
            })
        }
    }

    private fun createFuzzedArrayModel(arrayType: JavaArray, length: Int, values: List<UtModel>?): FuzzedValue {
        val elementClassId = (arrayType.elementType as WithClassId).classId
        return UtArrayModel(
            idGenerator.createId(),
            arrayType.classId,
            length,
            elementClassId.defaultValueModel(),
            values?.withIndex()?.associate { it.index to it.value }?.toMutableMap() ?: mutableMapOf()
        ).fuzzed {
            this.summary = "%var% = ${elementClassId.simpleName}[$length]"
        }
    }
}