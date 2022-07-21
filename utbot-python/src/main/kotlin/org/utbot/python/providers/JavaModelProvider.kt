package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonIntModel
import org.utbot.framework.plugin.api.PythonStrModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.providers.ConstantsModelProvider
import org.utbot.fuzzer.providers.StringConstantModelProvider
import java.math.BigInteger
import java.util.function.BiConsumer

object JavaModelProvider: ModelProvider {
    private val javaModelProvider = ModelProvider.of(
        ConstantsModelProvider,
        StringConstantModelProvider,
    )

    override fun generate(description: FuzzedMethodDescription) = sequence {
        val typeMap = mapOf(
            PythonIntModel.classId to intClassId,
            PythonStrModel.classId to stringClassId
        )
        val substitutedDescription = substituteType(description, typeMap)
        javaModelProvider.generate(substitutedDescription).forEach { fuzzedParameter ->
            val (index, fuzzedValue) = fuzzedParameter
            when (description.parameters[index]) {
                PythonIntModel.classId ->
                    ((fuzzedValue.model as? UtPrimitiveModel)?.value as? Int)?.let { intValue ->
                        yield(FuzzedParameter(
                            index,
                            PythonIntModel(BigInteger.valueOf(intValue.toLong())).fuzzed()
                        ))
                    }
                PythonStrModel.classId ->
                    ((fuzzedValue.model as? UtPrimitiveModel)?.value as? String)?.let { strValue ->
                        yield(FuzzedParameter(
                            index,
                            PythonStrModel(strValue).fuzzed()
                        ))
                    }
            }
        }
    }
}