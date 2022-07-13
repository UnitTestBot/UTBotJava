package org.utbot.fuzzer

import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.ClassId

fun interface ModelProvider {

    /**
     * Generates values for the method.
     *
     * @param description a fuzzed method description
     * @return sequence that produces [FuzzedParameter].
     */
    fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter>

    /**
     * Combines this model provider with `anotherModelProvider` into one instance.
     *
     * This model provider is called before `anotherModelProvider`.
     */
    fun with(anotherModelProvider: ModelProvider): ModelProvider {
        fun toList(m: ModelProvider) = if (m is Combined) m.providers else listOf(m)
        return Combined(toList(this) + toList(anotherModelProvider))
    }

    /**
     * Removes `anotherModelProvider` from current one.
     */
    fun except(anotherModelProvider: ModelProvider): ModelProvider {
        return except { it == anotherModelProvider }
    }

    /**
     * Removes `anotherModelProvider` from current one.
     */
    fun except(filter: (ModelProvider) -> Boolean): ModelProvider {
        return if (this is Combined) {
            Combined(providers.filterNot(filter))
        } else {
            Combined(if (filter(this)) emptyList() else listOf(this))
        }
    }

    /**
     * Creates [ModelProvider] that passes unprocessed classes to `modelProvider`.
     *
     * Returned model provider is called before `modelProvider` is called, therefore consumer will get values
     * from returned model provider and only after it calls `modelProvider`.
     *
     * @param modelProvider is called and every value of [ClassId] is collected which wasn't created by this model provider.
     */
    fun withFallback(modelProvider: ModelProvider) : ModelProvider {
        val thisModelProvider = this
        return ModelProvider { description ->
            sequence {
                val providedByDelegateMethodParameters = mutableSetOf<Int>()
                thisModelProvider.generate(description).forEach { (index, model) ->
                    providedByDelegateMethodParameters += index
                    yieldValue(index, model)
                }
                val missingParameters =
                    (0 until description.parameters.size).filter { !providedByDelegateMethodParameters.contains(it) }
                if (missingParameters.isNotEmpty()) {
                    val values = mutableMapOf<Int, MutableList<FuzzedValue>>()
                    modelProvider.generate(description).forEach { (i, m) -> values.computeIfAbsent(i) { mutableListOf() }.add(m) }
                    missingParameters.forEach { index ->
                        values[index]?.let { models ->
                            models.forEach { model ->
                                yieldValue(index, model)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates [ModelProvider] that passes unprocessed classes to `fallbackModelSupplier` function.
     *
     * This model provider is called before function is called, therefore consumer will get values
     * from this model provider and only after it created by `fallbackModelSupplier`.
     *
     * @param fallbackModelSupplier is called for every [ClassId] which wasn't created by this model provider.
     */
    fun withFallback(fallbackModelSupplier: (ClassId) -> UtModel?) : ModelProvider {
        return withFallback( ModelProvider { description ->
            sequence {
                description.parametersMap.forEach { (classId, indices) ->
                    fallbackModelSupplier(classId)?.let { model ->
                        indices.forEach { index ->
                            yieldValue(index, model.fuzzed())
                        }
                    }
                }
            }
        })
    }

    companion object {
        @JvmStatic
        fun of(vararg providers: ModelProvider): ModelProvider {
            return Combined(providers.toList())
        }

        suspend fun SequenceScope<FuzzedParameter>.yieldValue(index: Int, value: FuzzedValue) {
            yield(FuzzedParameter(index, value))
        }

        suspend fun SequenceScope<FuzzedParameter>.yieldAllValues(indices: List<Int>, models: Sequence<FuzzedValue>) {
            indices.forEach { index ->
                models.forEach { model ->
                    yieldValue(index, model)
                }
            }
        }

        suspend fun SequenceScope<FuzzedParameter>.yieldAllValues(indices: List<Int>, models: List<FuzzedValue>) {
            yieldAllValues(indices, models.asSequence())
        }
    }

    /**
     * Wrapper class that delegates implementation to the [providers].
     */
    private class Combined(val providers: List<ModelProvider>): ModelProvider {
        override fun generate(description: FuzzedMethodDescription): Sequence<FuzzedParameter> = sequence {
            providers.forEach { provider ->
                provider.generate(description).forEach {
                    yieldValue(it.index, it.value)
                }
            }
        }
    }

    fun UtModel.fuzzed(block: FuzzedValue.() -> Unit = {}): FuzzedValue = FuzzedValue(this, this@ModelProvider).apply(block)
}

inline fun <reified T> ModelProvider.exceptIsInstance(): ModelProvider {
    return except { it is T }
}