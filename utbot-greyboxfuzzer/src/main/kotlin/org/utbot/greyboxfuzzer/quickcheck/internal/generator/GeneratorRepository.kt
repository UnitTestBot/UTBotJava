package org.utbot.greyboxfuzzer.quickcheck.internal.generator

import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.generator.Generators
import org.utbot.greyboxfuzzer.quickcheck.generator.NullAllowed
import org.utbot.greyboxfuzzer.quickcheck.internal.Items.choose
import org.utbot.greyboxfuzzer.quickcheck.internal.ParameterTypeContext
import org.utbot.greyboxfuzzer.quickcheck.internal.Reflection
import org.utbot.greyboxfuzzer.quickcheck.internal.Weighted
import org.utbot.greyboxfuzzer.quickcheck.internal.Zilch
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field
import java.lang.reflect.Parameter
import java.lang.reflect.Type
import java.lang.annotation.Annotation as JavaAnnotation

open class GeneratorRepository private constructor(
    private val random: SourceOfRandomness,
    private val generators: MutableMap<Class<*>, MutableSet<Generator>>
) : Generators {
    constructor(random: SourceOfRandomness) : this(random, hashMapOf())

    fun getGenerators(): Map<Class<*>, MutableSet<Generator>> {
        return generators
    }

    fun addUserClassGenerator(forClass: Class<*>, source: Generator) {
        generators[forClass] = mutableSetOf(source)
    }

    fun removeGenerator(forClass: Class<*>) {
        generators.remove(forClass)
    }

    fun removeGeneratorForObjectClass() {
        generators.remove(Any::class.java)
    }

    fun register(source: Generator): GeneratorRepository {
        registerTypes(source)
        return this
    }

    fun register(source: Iterable<Generator>): GeneratorRepository {
        for (each in source) registerTypes(each)
        return this
    }

    private fun registerTypes(generator: Generator) {
        for (each in generator.types()) registerHierarchy(each, generator)
    }

    private fun registerHierarchy(type: Class<*>, generator: Generator) {
        maybeRegisterGeneratorForType(type, generator)
        when {
            type.superclass != null -> registerHierarchy(type.superclass, generator)
            type.isInterface -> registerHierarchy(Any::class.java, generator)
        }
        for (each in type.interfaces) registerHierarchy(each, generator)
    }

    private fun maybeRegisterGeneratorForType(type: Class<*>, generator: Generator) {
        if (generator.canRegisterAsType(type)) {
            registerGeneratorForType(type, generator)
        }
    }

    private fun registerGeneratorForType(type: Class<*>, generator: Generator) {
        val forType = generators.getOrPut(type) { mutableSetOf() }
        forType.add(generator)
    }

    override fun <T> type(type: Class<T>, vararg componentTypes: Class<*>): Generator {
        val generator = produceGenerator(
            ParameterTypeContext.forClass(type)
        )
        generator.addComponentGenerators(componentTypes.map { type(it) })
        return generator
    }

    override fun parameter(parameter: Parameter): Generator {
        return produceGenerator(
            ParameterTypeContext.forParameter(parameter).annotate(parameter)
        )
    }

    override fun field(field: Field): Generator {
        return produceGenerator(
            ParameterTypeContext.forField(field).annotate(field)
        )
    }

    override fun withRandom(other: SourceOfRandomness): Generators {
        return GeneratorRepository(other, generators)
    }

    fun produceGenerator(parameter: ParameterTypeContext): Generator {
        var generator = generatorFor(parameter)
        if (!isPrimitiveType(parameter.annotatedType().type)
            && hasNullableAnnotation(parameter.annotatedElement())
        ) {
            generator = NullableGenerator(generator)
        }
        generator.provide(this)
        //generator.configure(parameter.annotatedType())
        //if (parameter.topLevel()) generator.configure(parameter.annotatedElement())
        return generator
    }

    open fun generatorFor(parameter: ParameterTypeContext): Generator {
        return when {
            parameter.explicitGenerators().isNotEmpty() -> composeWeighted(parameter, parameter.explicitGenerators())
            parameter.isArray -> generatorForArrayType(parameter)
            else -> if (parameter.isEnum) {
                EnumGenerator(parameter.rawClass)
            } else {
                compose(parameter, matchingGenerators(parameter))
            }
        }
    }

    open fun generatorForArrayType(
        parameter: ParameterTypeContext
    ): ArrayGenerator {
        val component = parameter.arrayComponentContext()
        return ArrayGenerator(component.rawClass, generatorFor(component)).copy() as ArrayGenerator
    }

    private fun matchingGenerators(
        parameter: ParameterTypeContext
    ): List<Generator> {
        val matches = mutableListOf<Generator>()
        if (!hasGeneratorsFor(parameter)) {
            maybeAddGeneratorByNamingConvention(parameter, matches)
            maybeAddLambdaGenerator(parameter, matches)
            maybeAddMarkerInterfaceGenerator(parameter, matches)
        } else {
            maybeAddGeneratorsFor(parameter, matches)
        }
        require(matches.isNotEmpty()) {
            ("Cannot find generator for " + parameter.name()
                    + " of type " + parameter.type().typeName)
        }
        return matches
    }

    private fun maybeAddGeneratorByNamingConvention(
        parameter: ParameterTypeContext,
        matches: MutableList<Generator>
    ) {
        val genClass = try {
            Class.forName(parameter.rawClass.name + "Gen")
        } catch (noGenObeyingConvention: ClassNotFoundException) {
            return
        }
        if (Generator::class.java.isAssignableFrom(genClass)) {
            try {
                val generator = genClass.newInstance() as Generator
                if (generator.types().contains(parameter.rawClass)) {
                    matches += generator
                }
            } catch (e: IllegalAccessException) {
                throw IllegalStateException(
                    "Cannot instantiate " + genClass.name
                            + " using default constructor"
                )
            } catch (e: InstantiationException) {
                throw IllegalStateException(
                    "Cannot instantiate " + genClass.name
                            + " using default constructor"
                )
            }
        }
    }

    private fun maybeAddLambdaGenerator(
        parameter: ParameterTypeContext,
        matches: MutableList<Generator>
    ) {
        val method = Reflection.singleAbstractMethodOf(parameter.rawClass)
        if (method != null) {
            val returnType = parameter.methodReturnTypeContext(method)
            val returnTypeGenerator = generatorFor(returnType)
            matches += LambdaGenerator(parameter.rawClass, returnTypeGenerator).copy() as LambdaGenerator<*>
        }
    }

    private fun maybeAddMarkerInterfaceGenerator(
        parameter: ParameterTypeContext,
        matches: MutableList<Generator>
    ) {
        val rawClass = parameter.rawClass
        if (Reflection.isMarkerInterface(rawClass)) {
            matches += MarkerInterfaceGenerator(parameter.rawClass)
        }
    }

    private fun maybeAddGeneratorsFor(
        parameter: ParameterTypeContext,
        matches: MutableList<Generator>
    ) {
        val candidates = generatorsFor(parameter)
        val typeParameters = parameter.typeParameters
        if (typeParameters.isEmpty()) {
            matches.addAll(candidates)
        } else {
            for (each in candidates) {
                if (each.canGenerateForParametersOfTypes(typeParameters)) matches.add(each)
            }
        }
    }

    private fun compose(
        parameter: ParameterTypeContext,
        matches: List<Generator>
    ): Generator {
        val weightings = matches.map { Weighted(it, 1) }
        return composeWeighted(parameter, weightings)
    }

    private fun composeWeighted(
        parameter: ParameterTypeContext,
        matches: List<Weighted<Generator>>
    ): Generator {
        val forComponents = mutableListOf<Generator>()
        for (c in parameter.typeParameterContexts(random)) forComponents.add(generatorFor(c))
        for (each in matches) applyComponentGenerators(each.item, forComponents)
        return if (matches.size == 1) matches[0].item else CompositeGenerator(matches)
    }

    private fun applyComponentGenerators(
        generator: Generator,
        componentGenerators: List<Generator>
    ) {
        if (!generator.hasComponents()) return
        if (componentGenerators.isEmpty()) {
            val substitutes = mutableListOf<Generator>()
            val zilch = generatorFor(
                ParameterTypeContext.forClass(Zilch::class.java)
                    .allowMixedTypes(true)
            )
            for (i in 0 until generator.numberOfNeededComponents()) {
                substitutes.add(zilch)
            }
            generator.addComponentGenerators(substitutes)
        } else {
            generator.addComponentGenerators(componentGenerators)
        }
    }

    open fun generatorsFor(parameter: ParameterTypeContext): List<Generator> {
        var matches = generators[parameter.rawClass]
            ?: error("No generator for type: ${parameter.rawClass}")
        if (!parameter.allowMixedTypes()) {
            val match = choose(matches, random)
            matches = mutableSetOf(match)
        }
        return matches.map { it.copy() }
    }

    private fun hasGeneratorsFor(parameter: ParameterTypeContext): Boolean {
        return generators[parameter.rawClass] != null
    }

    companion object {
        private val NULLABLE_ANNOTATIONS = setOf(
            "javax.annotation.Nullable",  // JSR-305
            NullAllowed::class.java.canonicalName
        )


        private fun isPrimitiveType(type: Type): Boolean {
            return type is Class<*> && type.isPrimitive
        }

        private fun hasNullableAnnotation(element: AnnotatedElement?): Boolean {
            if (element == null) return false
            return element.annotations.map { it as JavaAnnotation }
                .map { it.annotationType() }
                .map { it.canonicalName }
                .any { NULLABLE_ANNOTATIONS.contains(it) }
        }
    }
}