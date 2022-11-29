package org.utbot.quickcheck.internal.generator

import org.utbot.engine.greyboxfuzzer.generator.GeneratorConfigurator
import org.utbot.engine.greyboxfuzzer.util.FuzzerIllegalStateException
import org.utbot.framework.plugin.api.UtModel
import org.utbot.quickcheck.generator.*
import org.utbot.quickcheck.internal.Items
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.internal.Weighted
import org.utbot.quickcheck.random.SourceOfRandomness
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.AnnotatedType

class CompositeGenerator(composed: List<Weighted<Generator>>) : Generator(Any::class.java) {
    private val composed: MutableList<Weighted<Generator>>
    private var previousChosenGenerator: Generator? = null

    init {
        this.composed = ArrayList(composed)
    }

    private fun regenerate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        val choice = Items.chooseWeighted(composed, random)
        previousChosenGenerator = choice
        return choice.generateImpl(random, status)
    }

    private fun modify(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        previousChosenGenerator ?: throw FuzzerIllegalStateException("Nothing to modify")
        return previousChosenGenerator!!.generateImpl(random, status)
    }

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return if (generationState == GenerationState.MODIFY) {
            modify(random, status)
        } else {
            regenerate(random, status)
        }
    }

    fun composed(index: Int): Generator {
        return composed[index].item
    }

    fun numberOfComposedGenerators(): Int {
        return composed.size
    }

    override fun provide(provided: Generators) {
        super.provide(provided)
        for (each in composed) each.item.provide(provided)
    }

    override fun configure(annotatedType: AnnotatedType?) {
        val candidates = composed.mapNotNull {
            try {
                it.item.configure(annotatedType)
                it
            } catch (e: GeneratorConfigurationException) {
                null
            }
        }
        installCandidates(candidates, annotatedType)
    }

    override fun configure(element: AnnotatedElement?) {
        val candidates = composed.mapNotNull {
            try {
                it.item.configure(element)
                it
            } catch (e: GeneratorConfigurationException) {
                null
            }
        }
        installCandidates(candidates, element)
    }

    override fun addComponentGenerators(newComponents: List<Generator>) {
        for (each in composed) {
            each.item.addComponentGenerators(newComponents)
        }
    }

    private fun installCandidates(
        candidates: List<Weighted<Generator>>,
        element: AnnotatedElement?
    ) {
        if (element == null) return
        if (candidates.isEmpty()) {
            throw GeneratorConfigurationException(
                String.format(
                    "None of the candidate generators %s"
                            + " understands all of the configuration annotations %s",
                    candidateGeneratorDescriptions(),
                    configurationAnnotationNames(element)
                )
            )
        }
        composed.clear()
        composed.addAll(candidates)
    }

    private fun candidateGeneratorDescriptions(): String {
        return composed.joinToString { it.item.javaClass.name }
    }

//    override fun copy(): Generator {
//        return (super.copy() as CompositeGenerator).also {
//            it.previousChosenGenerator = previousChosenGenerator
//            it.composed.addAll(composed)
//        }
//    }

    override fun copy(): Generator {
        val composedCopies = composed.map { Weighted(it.item.copy(), it.weight) }.toMutableList()
        val gen = Reflection.instantiate(CompositeGenerator::class.java.constructors.first(), composedCopies) as Generator
        return gen.also {
            it.generatedUtModel = generatedUtModel
            it.generationState = generationState
            it.nestedGenerators = nestedGenerators.map { it.copy() }.toMutableList()
            GeneratorConfigurator.configureGenerator(it, 100)
        }
    }

    companion object {
        private fun configurationAnnotationNames(
            element: AnnotatedElement
        ): List<String> = configurationAnnotationsOn(element)
            .map { a -> a.annotationType().name }
    }
}