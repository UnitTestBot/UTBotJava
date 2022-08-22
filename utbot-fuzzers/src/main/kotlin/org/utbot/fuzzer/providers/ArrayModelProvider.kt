package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues
import java.util.function.IntSupplier

class ArrayModelProvider(
    private val idGenerator: IdGenerator<Int>
) : ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap
            .asSequence()
            .filter { (classId, _) -> classId.isArray }
            .forEach { (arrayClassId, indices) ->
                yieldAllValues(indices, listOf(0, 10).map { arraySize ->
                    UtArrayModel(
                        id = idGenerator.createId(),
                        arrayClassId,
                        length = arraySize,
                        arrayClassId.elementClassId!!.defaultValueModel(),
                        mutableMapOf()
                    ).fuzzed {
                        this.summary = "%var% = ${arrayClassId.elementClassId!!.simpleName}[$arraySize]"
                    }
                })
            }
    }
}