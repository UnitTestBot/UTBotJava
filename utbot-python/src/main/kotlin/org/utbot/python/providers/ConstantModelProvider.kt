package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonBoolModel
import org.utbot.framework.plugin.api.PythonIntModel
import org.utbot.framework.plugin.api.PythonStrModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.providers.ConstantsModelProvider
import org.utbot.fuzzer.providers.StringConstantModelProvider
import java.math.BigInteger

object ConstantModelProvider: ModelProvider {
    private val javaModelProvider = ModelProvider.of(
        ConstantsModelProvider,
        StringConstantModelProvider,
    )

    override fun generate(description: FuzzedMethodDescription) = sequence {
        val typeMap = mapOf(
            PythonIntModel.classId to BigInteger::class.id,
            PythonStrModel.classId to String::class.id,
            PythonBoolModel.classId to Boolean::class.id
        )
        val substitutedDescription = substituteType(description, typeMap)
        javaModelProvider.generate(substitutedDescription).forEach { fuzzedParameter ->
            val (index, fuzzedValue) = fuzzedParameter
            when (description.parameters[index]) {
                PythonIntModel.classId ->
                    ((fuzzedValue.model as? UtPrimitiveModel)?.value as? BigInteger)?.let { intValue ->
                        yield(FuzzedParameter(
                            index,
                            PythonIntModel(intValue).fuzzed()
                        ))
                    }
                PythonStrModel.classId ->
                    ((fuzzedValue.model as? UtPrimitiveModel)?.value as? String)?.let { strValue ->
                        yield(FuzzedParameter(
                            index,
                            PythonStrModel(strValue).fuzzed()
                        ))
                    }
                PythonBoolModel.classId ->
                    ((fuzzedValue.model as? UtPrimitiveModel)?.value as? Boolean)?.let { boolValue ->
                        yield(FuzzedParameter(
                            index,
                            PythonBoolModel(boolValue).fuzzed()
                        ))
                    }
            }
        }
    }
}