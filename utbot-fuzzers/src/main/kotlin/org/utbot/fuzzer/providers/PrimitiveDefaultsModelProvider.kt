package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue

/**
 * Provides default values for primitive types.
 */
object PrimitiveDefaultsModelProvider : ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap.forEach { (classId, parameterIndices) ->
            valueOf(classId)?.let { model ->
                parameterIndices.forEach { index ->
                    yieldValue(index, model)
                }
            }
        }
    }

    fun valueOf(classId: ClassId): FuzzedValue? = when (classId) {
        booleanClassId -> UtPrimitiveModel(false).fuzzed { summary = "%var% = false" }
        byteClassId -> UtPrimitiveModel(0.toByte()).fuzzed { summary = "%var% = 0" }
        charClassId -> UtPrimitiveModel('\u0000').fuzzed { summary = "%var% = \u0000" }
        shortClassId -> UtPrimitiveModel(0.toShort()).fuzzed { summary = "%var% = 0" }
        intClassId -> UtPrimitiveModel(0).fuzzed { summary = "%var% = 0" }
        longClassId -> UtPrimitiveModel(0L).fuzzed { summary = "%var% = 0L" }
        floatClassId -> UtPrimitiveModel(0.0f).fuzzed { summary = "%var% = 0f" }
        doubleClassId -> UtPrimitiveModel(0.0).fuzzed { summary = "%var% = 0.0" }
        stringClassId -> UtPrimitiveModel("").fuzzed { summary = "%var% = \"\"" }
        else -> null
    }
}