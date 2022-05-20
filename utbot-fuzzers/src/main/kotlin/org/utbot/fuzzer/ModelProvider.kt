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
    fun with(anotherModelProvider: ModelProvider) : ModelProvider {
        return ModelProvider { description, consumer ->
            this@ModelProvider.generate(description, consumer)
            anotherModelProvider.generate(description, consumer)
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

}