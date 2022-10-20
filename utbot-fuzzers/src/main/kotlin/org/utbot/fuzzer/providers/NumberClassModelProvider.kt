package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isPrimitive
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.primitiveByWrapper
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.plugin.api.util.wrapperByPrimitive
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldValue
import org.utbot.fuzzer.objects.create
import org.utbot.fuzzer.types.*
import kotlin.random.Random

/**
 * Provides random implementation if current requested type is [Number].
 */
class NumberClassModelProvider(
    val idGenerator: IdentityPreservingIdGenerator<Int>,
    val random: Random,
) : ModelProvider {
    // byteClassId generates bad code because of type cast on method Byte.valueOf
    private val types: Set<Type> = setOf(/*byteClassId,*/ JavaShort, JavaInt, JavaLong, JavaFloat, JavaDouble)

    override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap[Number::class.id.toJavaType()]?.forEach { index ->
            val fuzzedValues = description.concreteValues.filter { types.contains(it.type) } +
                    (-5 until 5).map { FuzzedConcreteValue(types.random(random), it) }
            fuzzedValues.forEach { fuzzedValue ->
                val type = fuzzedValue.type
                if (type !is WithClassId) return@forEach
                val targetType = type.classId
                check(targetType.isPrimitive) { "$targetType is not primitive value" }
                val castedValue = castNumberIfPossible(fuzzedValue.value as Number, targetType)
                val targetValues = listOfNotNull(
                    castedValue.let(::UtPrimitiveModel),
                    ConstantsModelProvider.modifyValue(castedValue, fuzzedValue.fuzzedContext)?.model
                )
                // we use wrapper type to generate simple values,
                // because at the moment code generator uses reflection
                // if primitive types are provided
                val wrapperType = wrapperByPrimitive[targetType] ?: return@sequence
                targetValues.forEach { targetValue ->
                    yieldValue(index, wrapperType.create {
                        id = { idGenerator.createId() }
                        using static method(
                            classId = wrapperType,
                            name = "valueOf",
                            params = listOf(primitiveByWrapper[wrapperType]!!),
                            returns = wrapperType
                        ) with values(targetValue)
                    }.fuzzed { summary = "%var% = ${Number::class.simpleName}(${targetValue})" })
                }
            }
        }
    }

    private fun castNumberIfPossible(number: Number, classId: ClassId): Number = when (classId) {
        byteClassId -> number.toInt().toByte()
        shortClassId -> number.toInt().toShort()
        intClassId -> number.toInt()
        longClassId -> number.toLong()
        floatClassId -> number.toFloat()
        doubleClassId -> number.toDouble()
        else -> number
    }
}