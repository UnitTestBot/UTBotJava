package org.utbot.greyboxfuzzer.quickcheck.internal.generator

import org.javaruntype.type.TypeParameter
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.generator.Generators
import org.utbot.greyboxfuzzer.quickcheck.generator.NullAllowed
import org.utbot.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import org.utbot.greyboxfuzzer.util.classIdForType
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.AnnotatedType
import java.util.Optional

internal class NullableGenerator(private val delegate: Generator) : Generator(delegate.types()) {
    private var probabilityOfNull = Reflection.defaultValueOf(NullAllowed::class.java, "probability") as Double
    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return if (random.nextFloat(0f, 1f) < probabilityOfNull) {
            UtNullModel(classIdForType(types()[0]))
        } else {
            delegate.generateImpl(random, status)
        }
    }

    override fun canRegisterAsType(type: Class<*>): Boolean {
        return delegate.canRegisterAsType(type)
    }

    override fun hasComponents(): Boolean {
        return delegate.hasComponents()
    }

    override fun numberOfNeededComponents(): Int {
        return delegate.numberOfNeededComponents()
    }

    override fun addComponentGenerators(
        newComponents: List<Generator>
    ) {
        delegate.addComponentGenerators(newComponents)
    }

    override fun canGenerateForParametersOfTypes(typeParameters: List<TypeParameter<*>>): Boolean {
        return delegate.canGenerateForParametersOfTypes(typeParameters)
    }

    override fun configure(annotatedType: AnnotatedType?) {
        Optional.ofNullable(annotatedType!!.getAnnotation(NullAllowed::class.java))
            .ifPresent { allowed: NullAllowed -> this.configure(allowed) }
        delegate.configure(annotatedType)
    }

    override fun configure(element: AnnotatedElement?) {
        delegate.configure(element)
    }

    override fun provide(provided: Generators) {
        delegate.provide(provided)
    }

    private fun configure(allowed: NullAllowed) {
        if (allowed.probability >= 0 && allowed.probability <= 1) {
            probabilityOfNull = allowed.probability
        } else {
            throw IllegalArgumentException(
                "NullAllowed probability must be in the range [0, 1]"
            )
        }
    }
}