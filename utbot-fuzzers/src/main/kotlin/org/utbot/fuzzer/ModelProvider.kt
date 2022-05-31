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
    fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>)

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
     * Creates [ModelProvider] that passes unprocessed classes to `fallbackModelSupplier` function.
     *
     * This model provider is called before function is called, therefore consumer will get values
     * from this model provider and only after it created by `fallbackModelSupplier`.
     *
     * @param fallbackModelSupplier is called for every [ClassId] which wasn't created by this model provider.
     */
    fun withFallback(fallbackModelSupplier: (ClassId) -> UtModel?) : ModelProvider {
        return ModelProvider { description, consumer ->
            val providedByDelegateMethodParameters = mutableMapOf<Int, MutableList<UtModel>>()
            this@ModelProvider.generate(description) { index, model ->
                providedByDelegateMethodParameters.computeIfAbsent(index) { mutableListOf() }.add(model)
            }
            providedByDelegateMethodParameters.forEach { (index, models) ->
                models.forEach { model ->
                    consumer.accept(index, model)
                }
            }
            (0 until description.parameters.size)
                .filter { !providedByDelegateMethodParameters.containsKey(it) }
                .associateWith { description.parameters[it] }
                .forEach { (index, classId) ->
                    fallbackModelSupplier(classId)?.let { consumer.accept(index, it) }
                }
        }
    }

    companion object {
        @JvmStatic
        fun of(vararg providers: ModelProvider): ModelProvider {
            return Combined(providers.toList())
        }
    }

    /**
     * Wrapper class that delegates implementation to the [providers].
     */
    private class Combined(val providers: List<ModelProvider>): ModelProvider {
        override fun generate(description: FuzzedMethodDescription, consumer: BiConsumer<Int, UtModel>) {
            providers.forEach { provider ->
                provider.generate(description, consumer)
            }
        }
    }
}