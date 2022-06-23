package org.utbot.fuzzer

import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.ClassId
import java.util.function.BiConsumer

fun interface ModelProvider {

    /**
     * Generates values for the method.
     *
     * @param description a fuzzed method description
     * @param consumer accepts index in the parameter list and [UtModel] for this parameter.
     */
    fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, FuzzedValue>)

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
        return ModelProvider { description, consumer ->
            val providedByDelegateMethodParameters = mutableMapOf<Int, MutableList<FuzzedValue>>()
            this@ModelProvider.generate(description) { index, model ->
                providedByDelegateMethodParameters.computeIfAbsent(index) { mutableListOf() }.add(model)
            }
            providedByDelegateMethodParameters.forEach { (index, models) ->
                models.forEach { model ->
                    consumer.accept(index, model)
                }
            }
            val missingParameters =
                (0 until description.parameters.size).filter { !providedByDelegateMethodParameters.containsKey(it) }
            if (missingParameters.isNotEmpty()) {
                val values = mutableMapOf<Int, MutableList<FuzzedValue>>()
                modelProvider.generate(description) { i, m -> values.computeIfAbsent(i) { mutableListOf() }.add(m) }
                missingParameters.forEach { index ->
                    values[index]?.let { models ->
                        models.forEach { model ->
                            consumer.accept(index, model)
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
        return withFallback { description, consumer ->
            description.parametersMap.forEach { (classId, indices) ->
                fallbackModelSupplier(classId)?.let { model ->
                    indices.forEach { index ->
                        consumer.accept(index, model.fuzzed())
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun of(vararg providers: ModelProvider): ModelProvider {
            return Combined(providers.toList())
        }

        fun BiConsumer<Int, FuzzedValue>.consumeAll(indices: List<Int>, models: Sequence<FuzzedValue>) {
            models.forEach { model ->
                indices.forEach { index ->
                    accept(index, model)
                }
            }
        }

        fun BiConsumer<Int, FuzzedValue>.consumeAll(indices: List<Int>, models: List<FuzzedValue>) {
            consumeAll(indices, models.asSequence())
        }
    }

    /**
     * Wrapper class that delegates implementation to the [providers].
     */
    private class Combined(val providers: List<ModelProvider>): ModelProvider {
        override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, FuzzedValue>) {
            providers.forEach { provider ->
                provider.generate(description, consumer)
            }
        }
    }

    fun UtModel.fuzzed(block: FuzzedValue.() -> Unit = {}): FuzzedValue = FuzzedValue(this, this@ModelProvider).apply(block)
}

inline fun <reified T> ModelProvider.exceptIsInstance(): ModelProvider {
    return except { it is T }
}