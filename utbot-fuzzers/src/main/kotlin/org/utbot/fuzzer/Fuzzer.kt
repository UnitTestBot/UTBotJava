package org.utbot.fuzzer

import org.utbot.fuzzer.providers.ConstantsModelProvider
import org.utbot.fuzzer.providers.ObjectModelProvider
import org.utbot.fuzzer.providers.PrimitivesModelProvider
import org.utbot.fuzzer.providers.StringConstantModelProvider
import mu.KotlinLogging
import org.utbot.fuzzer.providers.ArrayModelProvider
import org.utbot.fuzzer.providers.CharToStringModelProvider
import org.utbot.fuzzer.providers.CollectionModelProvider
import org.utbot.fuzzer.providers.PrimitiveDefaultsModelProvider
import org.utbot.fuzzer.providers.EnumModelProvider
import org.utbot.fuzzer.providers.PrimitiveWrapperModelProvider
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.IntSupplier
import kotlin.random.Random

private val logger by lazy { KotlinLogging.logger {} }

fun fuzz(description: FuzzedMethodDescription, vararg modelProviders: ModelProvider): Sequence<List<FuzzedValue>> {
    if (modelProviders.isEmpty()) {
        throw IllegalArgumentException("At least one model provider is required")
    }

    val values = List<MutableList<FuzzedValue>>(description.parameters.size) { mutableListOf() }
    modelProviders.forEach { fuzzingProvider ->
        fuzzingProvider.generate(description).forEach { (index, model) ->
            values[index].add(model)
        }
    }
    description.parameters.forEachIndexed { index, classId ->
        val models = values[index]
        if (models.isEmpty()) {
            logger.warn { "There's no models provided classId=$classId. No suitable values are generated for ${description.name}" }
            return emptySequence()
        }
    }
    return CartesianProduct(values, Random(0L)).asSequence()
}

/**
 * Creates a model provider from a list of default providers.
 */
fun defaultModelProviders(idGenerator: IntSupplier = SimpleIdGenerator()): ModelProvider {
    return ModelProvider.of(
        ObjectModelProvider(idGenerator),
        CollectionModelProvider(idGenerator),
        ArrayModelProvider(idGenerator),
        EnumModelProvider(idGenerator),
        ConstantsModelProvider,
        StringConstantModelProvider,
        CharToStringModelProvider,
        PrimitivesModelProvider,
        PrimitiveWrapperModelProvider,
    )
}

/**
 * Creates a model provider for [ObjectModelProvider] that generates values for object constructor.
 */
fun objectModelProviders(idGenerator: IntSupplier = SimpleIdGenerator()): ModelProvider {
    return ModelProvider.of(
        CollectionModelProvider(idGenerator),
        ArrayModelProvider(idGenerator),
        EnumModelProvider(idGenerator),
        StringConstantModelProvider,
        CharToStringModelProvider,
        ConstantsModelProvider,
        PrimitiveDefaultsModelProvider,
        PrimitiveWrapperModelProvider,
    )
}

class SimpleIdGenerator : IntSupplier {
    private val id = AtomicInteger()
    override fun getAsInt() = id.incrementAndGet()
}