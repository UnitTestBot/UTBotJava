package org.utbot.fuzzer.providers

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedParameter
import org.utbot.fuzzer.FuzzedType
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
 * @param repeat if value greater than 1, [neededTypes] is duplicated, therefore [createModel] should accept `neededTypes.size * repeat` values
 * @param createModel lambda that takes subModels (they should have types listed in [neededTypes]) and generates a model using them
 */
data class ModelConstructor(
    val neededTypes: List<FuzzedType>,
    val repeat: Int = 1,
    val createModel: (subModels: List<FuzzedValue>) -> FuzzedValue,
) {
    var limit: Int = Int.MAX_VALUE
}

/**
 * Abstraction for providers that may call other providers recursively inside them. [generate] will firstly get possible
 * model constructors (provided by [generateModelConstructors]) and then fuzz parameters for each of them using synthetic method
 *
 * @param recursionDepthLeft maximum recursion level, i.e. maximum number of nested calls produced by this provider
 *
 * @property modelProviderForRecursiveCalls providers that can be called by this provider.
 * Note that if [modelProviderForRecursiveCalls] has instances of [RecursiveModelProvider] then this provider will use
 * their copies created by [newInstance] rather than themselves (this is important because we have to change some
 * properties like [recursionDepthLeft], [totalLimit], etc.)
 * @property fallbackProvider provider that will be used instead [modelProviderForRecursiveCalls] after reaching maximum recursion level
 * @property totalLimit maximum number of values produced by this provider
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

    /**
     * Creates instance of the class on which it is called, assuming that it will be called recursively from [parentProvider]
     */
    protected abstract fun newInstance(parentProvider: RecursiveModelProvider, constructor: ModelConstructor): RecursiveModelProvider

    /**
     * Creates [ModelProvider]s that will be used to generate values recursively. The order of elements in returned list is important:
     * only first [branchingLimit] constructors will be used, so you should place most effective providers first
     */
    protected abstract fun generateModelConstructors(
        description: FuzzedMethodDescription,
        parameterIndex: Int,
        classId: ClassId,
    ): Sequence<ModelConstructor>

    protected open fun copySettings(other: RecursiveModelProvider): RecursiveModelProvider {
        modelProviderForRecursiveCalls = other.modelProviderForRecursiveCalls
        fallbackProvider = other.fallbackProvider
        totalLimit = other.totalLimit
        branchingLimit = other.branchingLimit
        return this
    }

    final override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
        description.parameters.forEachIndexed { index, classId ->
            generateModelConstructors(description, index, classId)
                .take(branchingLimit)
                .forEach { creator ->
                    yieldAllValues(listOf(index), creator.recursiveCall(description))
                }
        }
    }.take(totalLimit)

    private fun ModelConstructor.recursiveCall(baseMethodDescription: FuzzedMethodDescription): Sequence<FuzzedValue> {
        // when no parameters are needed just call model creator once,
        // for example, if collection is empty or object has empty constructor
        if (neededTypes.isEmpty() || repeat == 0) {
            return sequenceOf(createModel(listOf()))
        }
        val syntheticMethodDescription = FuzzedMethodDescription(
            "<synthetic method: ${this::class.simpleName}>",
            voidClassId,
            (1..repeat).flatMap { neededTypes.map { it.classId } },
            baseMethodDescription.concreteValues
        ).apply {
            packageName = baseMethodDescription.packageName
            fuzzerType = { index ->
                neededTypes[index % neededTypes.size] // because we can repeat neededTypes several times
            }
        }
        return fuzz(syntheticMethodDescription, nextModelProvider(this))
            .map { createModel(it) }
            .take(limit)
    }

    private fun nextModelProvider(constructor: ModelConstructor): ModelProvider =
        if (recursionDepthLeft > 0) {
            modelProviderForRecursiveCalls.map {
                if (it is RecursiveModelProvider) {
                    it.newInstance(this, constructor)
                } else { it }
            }
        } else {
            modelProviderForRecursiveCalls
                .exceptIsInstance<RecursiveModelProvider>()
                .withFallback(fallbackProvider)
        }
}