package org.utbot.python

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.PythonIntModel
import org.utbot.framework.plugin.api.PythonStrModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.providers.*
import java.math.BigInteger
import java.util.function.BiConsumer

val notPythonModelProvider = ModelProvider.of(
    ConstantsModelProvider,
    StringConstantModelProvider,
    CharToStringModelProvider,
    PrimitivesModelProvider,
    PrimitiveWrapperModelProvider
)

object PythonModelProvider: ModelProvider {
    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, FuzzedValue>) {
        val typeMap = mapOf(
            PythonIntModel.classId to intClassId,
            PythonStrModel.classId to stringClassId
        )
        val substitutedDescription = substituteType(description, typeMap)
        notPythonModelProvider.generate(substitutedDescription) { index, fuzzedValue ->
            when (description.parameters[index]) {
                PythonIntModel.classId ->
                    ((fuzzedValue.model as? UtPrimitiveModel)?.value as? Long)?.let { long ->
                        consumer.accept(
                            index,
                            PythonIntModel(BigInteger.valueOf(long)).fuzzed()
                        )
                    }
                PythonStrModel.classId ->
                    consumer.accept(
                        index,
                        PythonStrModel((fuzzedValue.model as UtPrimitiveModel).value as String).fuzzed()
                    )
            }
        }
    }
}

fun substituteType(description: FuzzedMethodDescription, typeMap: Map<ClassId, ClassId>): FuzzedMethodDescription {
    val newReturnType = typeMap[description.returnType] ?: description.returnType
    val newParameters = description.parameters.map { typeMap[it] ?: it }
    val newConcreteValues = description.concreteValues.map { value ->
        val newType = typeMap[value.classId]
        if (newType != null) {
            FuzzedConcreteValue(newType, value.value, value.relativeOp)
        } else {
            value
        }
    }

    return FuzzedMethodDescription(description.name, newReturnType, newParameters, newConcreteValues)
}