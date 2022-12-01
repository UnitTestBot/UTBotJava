package org.utbot.engine.greyboxfuzzer.quickcheck.generator

import org.javaruntype.type.TypeParameter
import org.utbot.engine.greyboxfuzzer.util.FuzzerIllegalStateException
import org.utbot.framework.plugin.api.UtModel
import org.utbot.engine.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.lang.reflect.AnnotatedType
import java.util.Collections

/**
 * Produces values for property parameters of types that have parameterizations
 * that would also need generation, such as collections, maps, and predicates.
 *
 * @param <T> type of property parameter to apply this generator's values to
 * @param type class token for type of property parameter this generator
 * is applicable to
</T> */
abstract class ComponentizedGenerator constructor(type: Class<*>) : org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generator(type) {
    private val components: MutableList<org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generator> = ArrayList()

    /**
     * {@inheritDoc}
     *
     *
     * Generators of this type do not get called upon to generate values
     * for parameters of type [Object].
     */
    override fun canRegisterAsType(type: Class<*>): Boolean {
        return Any::class.java != type
    }

    override fun hasComponents(): Boolean {
        return true
    }

    override fun addComponentGenerators(
        newComponents: List<org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generator>
    ) {
        require(newComponents.size == numberOfNeededComponents()) {
            String.format(
                "Needed %d components for %s, but got %d",
                numberOfNeededComponents(),
                javaClass,
                newComponents.size
            )
        }
        components.clear()
        components.addAll(newComponents)
    }

    override fun canGenerateForParametersOfTypes(
        typeParameters: List<TypeParameter<*>>
    ): Boolean {
        return numberOfNeededComponents() == typeParameters.size
    }

    override fun provide(provided: org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generators) {
        super.provide(provided)
        for (each in components) {
            each.provide(provided)
        }
    }

    abstract fun createModifiedUtModel(random: SourceOfRandomness, status: org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus): UtModel
    protected open fun modify(
        random: SourceOfRandomness,
        status: org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationStatus
    ): UtModel {
        val cachedModel = generatedUtModel ?: throw FuzzerIllegalStateException("Nothing to modify")
        val randomNestedGenerator = nestedGeneratorsRecursiveWithoutThis().randomOrNull() ?: return cachedModel
        getAllGeneratorsBetween(this, randomNestedGenerator)?.forEach {
            it.generationState = org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationState.MODIFYING_CHAIN
        }
        randomNestedGenerator.generationState =
            org.utbot.engine.greyboxfuzzer.quickcheck.generator.GenerationState.REGENERATE
        return createModifiedUtModel(random, status)
    }
    override fun configure(annotatedType: AnnotatedType?) {
        super.configure(annotatedType)
        val annotatedComponentTypes = Reflection.annotatedComponentTypes(annotatedType)
        if (annotatedComponentTypes.size == components.size) {
            for (i in components.indices) {
                components[i].configure(annotatedComponentTypes[i])
            }
        }
    }

    /**
     * @return this generator's component generators
     */
    fun componentGenerators(): List<org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generator> {
        return Collections.unmodifiableList(components)
    }

    override fun copy(): org.utbot.engine.greyboxfuzzer.quickcheck.generator.Generator {
        return (super.copy() as org.utbot.engine.greyboxfuzzer.quickcheck.generator.ComponentizedGenerator).also {
            it.components.addAll(components.map { it.copy() })
        }
    }
}