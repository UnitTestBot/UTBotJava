package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.ModelProvider.Companion.yieldAllValues
import org.utbot.fuzzer.exceptIsInstance
import org.utbot.fuzzer.fuzz
import org.utbot.fuzzer.modelProviderForRecursiveCalls

/**
 * Auxiliary data class that stores information describing how to construct model from parts (submodels)
 *
 * @param neededTypes list containing type ([ClassId]) of each submodel
 * @param createModel lambda that takes subModels (they should have types listed in [neededTypes]) and generates a model using them
 */
data class ModelConstructor(
    val neededTypes: List<ClassId>,
    val createModel: (subModels: List<FuzzedValue>) -> FuzzedValue
)

/**
 * Abstraction for providers that may call other providers recursively inside them. [generate] will firstly get possible
 * model constructors (provided by [generateModelConstructors]) and then fuzz parameters for each of them using synthetic method
 *
 * @param recursionDepthLeft maximum recursion level, i.e. maximum number of nested calls produced by this provider
 *
 * @property modelProviderForRecursiveCalls providers that can be called by this provider.
 * Note that if [modelProviderForRecursiveCalls] has instances of [RecursiveModelProvider] then this provider will use
 * their copies created by [createNewInstance] rather than themselves (this is important because we have to change some
 * properties like [recursionDepthLeft], [totalLimit], etc.)
 *
 * @property fallbackProvider provider that will be used instead [modelProviderForRecursiveCalls] after reaching maximum recursion level
 *
 * @property totalLimit maximum number of parameters produced by this provider
 *
 * @property branchingLimit maximum number of [ModelConstructor]s used by [generate] (see [generateModelConstructors])
 */
abstract class RecursiveModelProvider(
    val idGenerator: IdentityPreservingIdGenerator<Int>,
    val recursionDepthLeft: Int
) : ModelProvider {
    var modelProviderForRecursiveCalls: ModelProvider = modelProviderForRecursiveCalls(idGenerator, recursionDepthLeft - 1)

    var fallbackProvider: ModelProvider = NullModelProvider

    var totalLimit: Int = 1000

    var branchingLimit: Int = Int.MAX_VALUE

    private fun getModelProvider(numOfBranches: Int): ModelProvider =
        if (recursionDepthLeft > 0)
            modelProviderForRecursiveCalls.map {
                if (it is RecursiveModelProvider)
                    it.createNewInstance(this, totalLimit / numOfBranches)
                else
                    it
            }
        else
            modelProviderForRecursiveCalls
                .exceptIsInstance<RecursiveModelProvider>()
                .withFallback(fallbackProvider)

    /**
     * Creates instance of the class on which it is called, assuming that it will be called recursively from [parentProvider]
     */
    protected abstract fun createNewInstance(parentProvider: RecursiveModelProvider, newTotalLimit: Int): RecursiveModelProvider

    /**
     * Creates [ModelProvider]s that will be used to generate values recursively. The order of elements in returned list is important:
     * only first [branchingLimit] constructors will be used, so you should place most effective providers first
     */
    protected abstract fun generateModelConstructors(description: FuzzedMethodDescription, classId: ClassId): List<ModelConstructor>

    protected fun copySettingsFrom(otherProvider: RecursiveModelProvider): RecursiveModelProvider {
        modelProviderForRecursiveCalls = otherProvider.modelProviderForRecursiveCalls
        fallbackProvider = otherProvider.fallbackProvider
        totalLimit = otherProvider.totalLimit
        branchingLimit = otherProvider.branchingLimit
        return this
    }

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
            "<synthetic method for RecursiveModelProvider>", //TODO: maybe add more info here
            voidClassId,
            types,
            baseMethodDescription.concreteValues
        ).apply {
            packageName = baseMethodDescription.packageName
        }
        return fuzz(syntheticMethodDescription, modelProvider)
    }
}