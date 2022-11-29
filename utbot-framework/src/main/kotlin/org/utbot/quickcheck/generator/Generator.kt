package org.utbot.quickcheck.generator

import org.javaruntype.type.TypeParameter
import org.javaruntype.type.Types
import org.javaruntype.type.WildcardTypeParameter
import org.utbot.engine.greyboxfuzzer.generator.GeneratorConfigurator
import org.utbot.engine.greyboxfuzzer.util.FuzzerIllegalStateException
import org.utbot.engine.greyboxfuzzer.util.getImplementersOfWithChain
import org.utbot.engine.greyboxfuzzer.util.removeIfAndReturnRemovedElements
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.quickcheck.internal.Reflection
import org.utbot.quickcheck.internal.ReflectionException
import org.utbot.quickcheck.random.SourceOfRandomness
import soot.SootClass
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.AnnotatedType
import java.lang.reflect.Method
import java.util.Collections
import java.lang.annotation.Annotation as JavaAnnotation

/**
 * Produces values for property parameters.
 *
 */
abstract class Generator protected constructor(types: List<Class<*>>) : Gen {
    private val types: MutableList<Class<*>> = ArrayList()
    private var repo: Generators? = null
    var generatedUtModel: UtModel? = null
    var generationState = GenerationState.REGENERATE
    var nestedGenerators = mutableListOf<Generator>()
    lateinit var generatorContext: GeneratorContext

    open fun generateImpl(random: SourceOfRandomness, status: GenerationStatus): UtModel {
        if (generatorContext.checkPoint()) {
            return UtNullModel(objectClassId)
        }
        return when (generationState) {
            GenerationState.REGENERATE -> {
                generate(random, status).also {
                    generatedUtModel = it
                    nestedGeneratorsRecursive().forEach {
                        it.generationState = GenerationState.CACHE
                    }
                }
            }
            GenerationState.CACHE -> {
                generatedUtModel ?: throw FuzzerIllegalStateException("No cached model")
            }
            GenerationState.MODIFY -> {
                withModification {
                    generate(random, status).also { generatedUtModel = it }
                }
            }
            GenerationState.MODIFYING_CHAIN -> {
                generate(random, status).also {
                    generatedUtModel = it
                    nestedGeneratorsRecursive().forEach {
                        it.generationState = GenerationState.CACHE
                    }
                }
            }
        }
    }

    private fun flattenedTo(destination: MutableList<Generator>) {
        destination.add(this)
        nestedGenerators.forEach { it.flattenedTo(destination) }
    }

    fun nestedGeneratorsRecursive(): List<Generator> {
        val allGenerators = mutableListOf<Generator>()
        this.flattenedTo(allGenerators)
        return allGenerators
    }

    fun nestedGeneratorsRecursiveWithoutThis() = nestedGeneratorsRecursive().filter { it != this }

    private fun genRandomNestedGenerator(): Generator? {
        val queue = ArrayDeque<Generator>()
        val res = mutableListOf<Generator>()
        queue.add(this)
        while (queue.isNotEmpty()) {
            val el = queue.removeFirst()
            res.add(el)
        }
        return res.randomOrNull()
    }

    protected fun getAllGeneratorsBetween(start: Generator, end: Generator): List<Generator>? {
        val res = mutableListOf(mutableListOf(start))
        val queue = ArrayDeque<Generator>()
        queue.add(start)
        while (queue.isNotEmpty()) {
            val curGenerator = queue.removeFirst()
            if (curGenerator == end) break
            val nestedGenerators = curGenerator.nestedGenerators
            if (nestedGenerators.isEmpty()) continue
            val oldLists = res.removeIfAndReturnRemovedElements { it.last() == curGenerator }
            for (implementer in nestedGenerators) {
                queue.add(implementer)
                oldLists.forEach { res.add((it + listOf(implementer)).toMutableList()) }
            }
        }
        return res.find { it.last() == end }?.drop(1)?.dropLast(1)
    }

    private fun getAllGeneratorsBetween(currentPath: MutableList<Generator>, end: Generator) {

    }

    /**
     * @param type class token for type of property parameter this generator is
     * applicable to
     */
    protected constructor(type: Class<*>) : this(listOf(type))

    /**
     * Used for generators of primitives and their wrappers. For example, a
     * `Generator<Integer>` can be used for property parameters of type
     * `Integer` or `int`.
     *
     * @param types class tokens for type of property parameter this generator
     * is applicable to
     */
    init {
        this.types.addAll(types)
    }

    /**
     * @return class tokens for the types of property parameters this generator
     * is applicable to
     */
    fun types(): List<Class<*>> {
        return Collections.unmodifiableList(types)
    }

    /**
     * Tells whether this generator is allowed to be used for property
     * parameters of the given type.
     *
     * @param type type against which to test this generator
     * @return `true` if the generator is allowed to participate in
     * generating values for property parameters of `type`
     */
    open fun canRegisterAsType(type: Class<*>): Boolean {
        return true
    }

    /**
     *
     * This is intended for use only by junit-quickcheck itself, and not by
     * creators of custom generators.
     *
     * @return whether this generator has component generators, such as for
     * those generators that produce lists or maps
     * @see .addComponentGenerators
     */
    open fun hasComponents(): Boolean {
        return false
    }

    /**
     *
     * This is intended for use only by junit-quickcheck itself, and not by
     * creators of custom generators.
     *
     * @return how many component generators this generator needs
     * @see .addComponentGenerators
     */
    open fun numberOfNeededComponents(): Int {
        return 0
    }

    /**
     *
     * Adds component generators to this generator.
     *
     *
     * Some generators need component generators to create proper values.
     * For example, list generators require a single component generator in
     * order to generate elements that have the type of the list parameter's
     * type argument.
     *
     *
     * This is intended for use only by junit-quickcheck itself, and not by
     * creators of custom generators.
     *
     * @param newComponents component generators to add
     */
    open fun addComponentGenerators(newComponents: List<Generator>) {
        // do nothing by default
    }

    /**
     * @param typeParameters a list of generic type parameters
     * @return whether this generator can be considered for generating values
     * for property parameters that have the given type parameters in their
     * signatures
     */
    open fun canGenerateForParametersOfTypes(typeParameters: List<TypeParameter<*>>): Boolean {
        return true
    }

    /**
     *
     * Configures this generator using annotations from a given annotated
     * type.
     *
     *
     * This method considers only annotations that are themselves marked
     * with [GeneratorConfiguration].
     *
     *
     * By default, the generator will configure itself using this
     * procedure:
     *
     *  * For each of the given annotations:
     *
     *  * Find a `public` method on the generator named
     * `configure`, that accepts a single parameter of the
     * annotation type
     *  * Invoke the `configure` method reflectively, passing the
     * annotation as the argument
     *
     *
     *
     *
     * @param annotatedType a type usage
     * @throws GeneratorConfigurationException if the generator does not
     * "understand" one of the generation configuration annotations on
     * the annotated type
     */
    open fun configure(annotatedType: AnnotatedType?) {
        configureStrict(collectConfigurationJavaAnnotations(annotatedType))
    }

    /**
     * @param element an annotated program element
     */
    open fun configure(element: AnnotatedElement?) {
        configureLenient(collectConfigurationJavaAnnotations(element))
    }

    /**
     *
     * Supplies the available generators to this one.
     *
     *
     * This is intended for use only by junit-quickcheck itself, and not by
     * creators of custom generators.
     *
     * @param provided repository of available generators
     */
    open fun provide(provided: Generators) {
        repo = provided
    }

    fun isGeneratorContextInitialized() = this::generatorContext.isInitialized
    /**
     * Used by the framework to make a copy of the receiver.
     *
     * @return a copy of the receiver
     */
    open fun copy(): Generator {
        return Reflection.instantiate(javaClass).also {
            it.generatedUtModel = generatedUtModel
            it.generationState = generationState
            it.nestedGenerators = nestedGenerators.map { it.copy() }.toMutableList()
            if (isGeneratorContextInitialized()) {
                it.generatorContext = generatorContext
            }
            GeneratorConfigurator.configureGenerator(it, 95)
        }
    }

    /**
     * @return an access point for the available generators
     */
    protected fun gen(): Generators? {
        return repo
    }

    /**
     * @param random a source of randomness used when locating other
     * generators
     * @return an access point for the available generators
     */
    protected fun gen(random: SourceOfRandomness?): Generators {
        return repo!!.withRandom(random!!)
    }

    private fun collectConfigurationJavaAnnotations(
        element: AnnotatedElement?
    ): Map<Class<out JavaAnnotation>, JavaAnnotation> {
        if (element == null) return emptyMap()
        val configs = configurationAnnotationsOn(element)
        return configs.associateBy { it.annotationType() as Class<out JavaAnnotation> }
    }

    private fun configureStrict(byType: Map<Class<out JavaAnnotation>, JavaAnnotation>) {
        for ((key, value) in byType) configureStrict(key, value)
    }

    private fun configureStrict(
        annotationType: Class<out JavaAnnotation>,
        configuration: JavaAnnotation
    ) {
        configure(
            annotationType,
            configuration
        ) { ex: ReflectionException? ->
            throw GeneratorConfigurationException(
                String.format(
                    "Generator %s does not understand configuration annotation %s",
                    javaClass.name,
                    annotationType.name
                ),
                ex
            )
        }
    }

    private fun configureLenient(byType: Map<Class<out JavaAnnotation>, JavaAnnotation>) {
        for ((key, value) in byType) configureLenient(key, value)
    }

    private fun configureLenient(
        annotationType: Class<out JavaAnnotation>,
        configuration: JavaAnnotation
    ) {
        configure(annotationType, configuration) { ex: ReflectionException? -> }
    }

    private fun configure(
        annotationType: Class<out JavaAnnotation>,
        configuration: JavaAnnotation,
        exceptionHandler: (ReflectionException) -> Unit
    ) {
        var configurer: Method? = null
        try {
            configurer = Reflection.findMethod(javaClass, "configure", annotationType)
        } catch (ex: ReflectionException) {
            exceptionHandler(ex)
        }
        if (configurer != null) Reflection.invoke(configurer, this, configuration)
    }

    protected fun <T> withModification(block: () -> T): T {
        generationState = GenerationState.MODIFY
        return block.invoke().also { generationState = GenerationState.CACHE }
    }

    companion object {
        /**
         * @param parameter a generic type parameter
         * @param clazz a type
         * @return whether the type is compatible with the generic type parameter
         * @see .canGenerateForParametersOfTypes
         */
        protected fun compatibleWithTypeParameter(
            parameter: TypeParameter<*>,
            clazz: Class<*>?
        ): Boolean {
            return (parameter is WildcardTypeParameter
                    || parameter.type.isAssignableFrom(Types.forJavaLangReflectType(clazz)))
        }

        /**
         * Gives a list of the [GeneratorConfiguration] annotations present
         * on the given program element.
         *
         * @param element an annotated program element
         * @return what configuration annotations are present on that element
         */
        @JvmStatic
        protected fun configurationAnnotationsOn(element: AnnotatedElement): List<JavaAnnotation> =
            Reflection.allAnnotations(element)
                .filter {
                    it.annotationType().isAnnotationPresent(
                        GeneratorConfiguration::class.java
                    )
                }
    }
}