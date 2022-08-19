package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.fuzz
import org.utbot.fuzzer.nonRecursiveProviders
import org.utbot.fuzzer.recursiveModelProviders

// TODO: maybe use `recursionDepth` instead of `recursion` here and in [recursiveModelProviders]?
abstract class RecursiveModelProvider(
    protected val idGenerator: IdentityPreservingIdGenerator<Int>,
    protected val recursion: Int
): ModelProvider {
    // TODO: currently it is var due to tests, maybe we can make it private val?
    var modelProviderForRecursiveCalls: ModelProvider =
        if (recursion > 0)
            nonRecursiveProviders(idGenerator).with(recursiveModelProviders(idGenerator, recursion - 1))
        else
            nonRecursiveProviders(idGenerator)

    protected fun generateRecursiveProvider(
        baseProvider: ModelProvider = modelProviderForRecursiveCalls,
        fallbackProvider: ModelProvider = NullModelProvider
    ): ModelProvider {
        return if (recursion > 0)
            baseProvider
        else
            baseProvider.withFallback(fallbackProvider)
    }

    protected fun fuzzValuesRecursively(
        types: List<ClassId>,
        baseMethodDescription: FuzzedMethodDescription,
        modelProvider: ModelProvider,
        generatedValuesName: String
    ): Sequence<List<FuzzedValue>> {
        val syntheticMethodDescription = FuzzedMethodDescription(
            "<synthetic method for fuzzing $generatedValuesName",
            voidClassId,
            types,
            baseMethodDescription.concreteValues
        ).apply {
            packageName = baseMethodDescription.packageName
        }
        return fuzz(syntheticMethodDescription, modelProvider)
    }
}