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
import org.utbot.framework.plugin.api.util.primitiveByWrapper
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue
import org.utbot.fuzzer.types.WithClassId
import kotlin.random.Random

/**
 * Provides default values for primitive types.
 */
class PrimitiveRandomModelProvider(val random: Random, val size: Int = 5) : ModelProvider {
    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap.forEach { (type, parameterIndices) ->
            if (type !is WithClassId) return@forEach
            for (i in 1..size) {
                valueOf(primitiveByWrapper[type.classId] ?: type.classId)?.let { model ->
                    parameterIndices.forEach { index ->
                        yieldValue(index, model)
                    }
                }
            }
        }
    }

    fun valueOf(classId: ClassId): FuzzedValue? = when (classId) {
        booleanClassId -> random.nextBoolean().let { v -> UtPrimitiveModel(v).fuzzed { summary = "%var% = $v" } }
        byteClassId -> random.nextInt(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).let { v ->
            UtPrimitiveModel(v.toByte()).fuzzed { summary = "%var% = random byte" }
        }
        charClassId -> random.nextInt(1, 256).let { v ->
            UtPrimitiveModel(v.toChar()).fuzzed { summary = "%var% = random char" }
        }
        shortClassId -> random.nextInt(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).let { v ->
            UtPrimitiveModel(v.toShort()).fuzzed { summary = "%var% = random short" }
        }
        intClassId -> random.nextInt().let { v ->
            UtPrimitiveModel(v).fuzzed { summary = "%var% = random integer" }
        }
        longClassId -> random.nextLong().let { v ->
            UtPrimitiveModel(v).fuzzed { summary = "%var% = random long" }
        }
        floatClassId -> random.nextFloat().let { v ->
            UtPrimitiveModel(v).fuzzed { summary = "%var% = random float" }
        }
        doubleClassId -> random.nextDouble().let { v ->
            UtPrimitiveModel(0.0).fuzzed { summary = "%var% = random double" }
        }
        stringClassId -> (1..5).map { random.nextInt('a'.code, 'z'.code).toChar() }.joinToString("").let { s ->
            UtPrimitiveModel(s).fuzzed { summary = "%var% = random string" }
        }
        else -> null
    }
}