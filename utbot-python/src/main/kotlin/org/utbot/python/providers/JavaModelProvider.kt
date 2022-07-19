package org.utbot.python.providers

import org.utbot.framework.plugin.api.PythonIntModel
import org.utbot.framework.plugin.api.PythonStrModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedMethodDescription
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

    override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, FuzzedValue>) {
        val typeMap = mapOf(
            PythonIntModel.classId to intClassId,
            PythonStrModel.classId to stringClassId
        )
        val substitutedDescription = substituteType(description, typeMap)
        javaModelProvider.generate(substitutedDescription) { index, fuzzedValue ->
            when (description.parameters[index]) {
                PythonIntModel.classId ->
                    ((fuzzedValue.model as? UtPrimitiveModel)?.value as? Int)?.let { int_val ->
                        consumer.accept(
                            index,
                            PythonIntModel(BigInteger.valueOf(int_val.toLong())).fuzzed()
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