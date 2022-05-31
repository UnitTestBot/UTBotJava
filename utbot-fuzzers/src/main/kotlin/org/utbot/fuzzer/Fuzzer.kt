package org.utbot.fuzzer

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.fuzzer.providers.ConstantsModelProvider
import org.utbot.fuzzer.providers.ObjectModelProvider
import org.utbot.fuzzer.providers.PrimitivesModelProvider
import org.utbot.fuzzer.providers.StringConstantModelProvider
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.ToIntFunction
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

fun fuzz(description: FuzzedMethodDescription, vararg modelProviders: ModelProvider): Sequence<List<UtModel>> {
    val values = List<MutableList<UtModel>>(description.parameters.size) { mutableListOf() }
    modelProviders.forEach { fuzzingProvider ->
        fuzzingProvider.generate(description) { index, model ->
            values[index].add(model)
        }
    }
    description.parameters.forEachIndexed { index, classId ->
        val models = values[index]
        if (models.isEmpty()) {
            logger.warn { "There's no models provided classId=$classId. Null or default value will be provided" }
            models.add(classId.defaultValueModel())
        }
    }
    return CartesianProduct(values, Random(0L)).asSequence()
}

fun defaultModelProviders(idGenerator: ToIntFunction<ClassId> = SimpleIdGenerator()): ModelProvider {
    return ObjectModelProvider(idGenerator)
        .with(ConstantsModelProvider)
        .with(StringConstantModelProvider)
        .with(PrimitivesModelProvider)
}

private class SimpleIdGenerator : ToIntFunction<ClassId> {
    private val id = AtomicInteger()
    override fun applyAsInt(value: ClassId?) = id.incrementAndGet()
}