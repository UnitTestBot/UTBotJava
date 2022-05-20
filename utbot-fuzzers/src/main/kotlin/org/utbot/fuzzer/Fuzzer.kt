package org.utbot.fuzzer

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.ToIntFunction

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
    return CartesianProduct(values).asSequence()
}

fun defaultModelProviders(idGenerator: ToIntFunction<ClassId> = SimpleIdGenerator()): ModelProvider {
    return ObjectModelProvider(idGenerator)
        .with(PrimitivesModelProvider)
        .with(ConstantsModelProvider)
}

private class SimpleIdGenerator : ToIntFunction<ClassId> {
    private val id = AtomicInteger()
    override fun applyAsInt(value: ClassId?) = id.incrementAndGet()
}