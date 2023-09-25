package org.utbot.fuzzing.spring.decorators

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.providers.findMethodsToModifyWith

/**
 * Value provider that is a buddy for another provider
 * that keeps all it's functionality and also allows
 * to use methods to mutate field states of an object.
 *
 * NOTE!!!
 * Instances represented by [UtAssembleModel] only can be mutated with methods.
 */
class ModifyingWithMethodsProviderWrapper(
    private val classUnderTest: ClassId,
    delegate: JavaValueProvider
) : ValueProviderDecorator<FuzzedType, FuzzedValue, FuzzedDescription>(delegate) {

    override fun wrap(provider: JavaValueProvider): JavaValueProvider =
        ModifyingWithMethodsProviderWrapper(classUnderTest, provider)

    override fun generate(description: FuzzedDescription, type: FuzzedType): Sequence<Seed<FuzzedType, FuzzedValue>> =
        delegate
            .generate(description, type)
            .map { seed ->
                if (seed is Seed.Recursive) {
                    Seed.Recursive(
                        construct = seed.construct,
                        modify = seed.modify +
                                findMethodsToModifyWith(description, type.classId, classUnderTest)
                                    .asSequence()
                                    .map { md ->
                                        Routine.Call(md.parameterTypes) { self, values ->
                                            val model = self.model as UtAssembleModel
                                            model.modificationsChain as MutableList +=
                                                UtExecutableCallModel(
                                                    model,
                                                    md.method.executableId,
                                                    values.map { it.model }
                                                )
                                        }
                                    },
                        empty = seed.empty,
                    )
                } else seed
            }
}