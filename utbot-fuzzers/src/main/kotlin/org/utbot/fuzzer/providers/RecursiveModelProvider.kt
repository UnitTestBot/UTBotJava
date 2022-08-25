package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues
import org.utbot.fuzzer.defaultModelProviders
import org.utbot.fuzzer.exceptIsInstance
import org.utbot.fuzzer.fuzz

data class ModelConstructor(
    val neededTypes: List<ClassId>,
    val createModel: (List<FuzzedValue>) -> FuzzedValue
)

abstract class RecursiveModelProvider(
    protected val idGenerator: IdentityPreservingIdGenerator<Int>,
    protected val recursionDepthLeft: Int
): ModelProvider {
    var modelProviderForRecursiveCalls: ModelProvider = defaultModelProviders(idGenerator, recursionDepthLeft - 1)

    var fallbackProvider: ModelProvider = NullModelProvider

    var totalLimit: Int = 1000

    var branchingLimit: Int = 10 //Int.MAX_VALUE

    private fun getModelProvider(numOfBranches: Int): ModelProvider =
        if (recursionDepthLeft > 0)
            modelProviderForRecursiveCalls.map {
                if (it is RecursiveModelProvider)
                    it.copy(idGenerator, recursionDepthLeft - 1).apply {
                        modelProviderForRecursiveCalls = this@RecursiveModelProvider.modelProviderForRecursiveCalls
                        fallbackProvider = this@RecursiveModelProvider.fallbackProvider
                        totalLimit = this@RecursiveModelProvider.totalLimit / numOfBranches
                        branchingLimit = this@RecursiveModelProvider.branchingLimit
                    }
                else
                    it
            }
        else
            modelProviderForRecursiveCalls
                .exceptIsInstance<RecursiveModelProvider>()
                .withFallback(fallbackProvider)

    abstract fun copy(idGenerator: IdentityPreservingIdGenerator<Int>, recursionDepthLeft: Int): RecursiveModelProvider

    abstract fun generateModelConstructors(description: FuzzedMethodDescription, clazz: ClassId): List<ModelConstructor>

    final override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parametersMap.asSequence().forEach { (classId, indices) ->
            val constructors = generateModelConstructors(description, classId).take(branchingLimit)
            constructors.asSequence().forEach { constructor ->
                val modelProvider = getModelProvider(constructors.size)
                val valuesSets =
                    fuzzValuesRecursively(constructor.neededTypes, description, modelProvider)
                        .take(totalLimit / constructors.size)
                yieldAllValues(indices, valuesSets.map(constructor.createModel))
            }
        }
    }.take(totalLimit)

    protected fun fuzzValuesRecursively(
        types: List<ClassId>,
        baseMethodDescription: FuzzedMethodDescription,
        modelProvider: ModelProvider,
    ): Sequence<List<FuzzedValue>> {
        if (types.isEmpty())
            return sequenceOf(listOf())
        val syntheticMethodDescription = FuzzedMethodDescription(
            "<synthetic method for RecursiveModelProvider>", // TODO: maybe add something here
            voidClassId,
            types,
            baseMethodDescription.concreteValues
        ).apply {
            packageName = baseMethodDescription.packageName
        }
        return fuzz(syntheticMethodDescription, modelProvider)
    }
}