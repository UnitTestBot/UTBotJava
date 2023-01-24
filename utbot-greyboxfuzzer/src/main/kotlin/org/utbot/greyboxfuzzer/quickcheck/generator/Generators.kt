package org.utbot.greyboxfuzzer.quickcheck.generator

import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.lang.reflect.Field
import java.lang.reflect.Parameter

/**
 * An access point for available generators.
 */
interface Generators {
    /**
     *
     * Gives a generator that can produce values of the given type,
     * parameterized by the given "component" types.
     *
     * @param <T> type of objects produced by the resulting generator
     * @param type a type
     * @param componentTypes types for the "components" of the type, if any
     * @return generator that can produce values of that type
     * @see ComponentizedGenerator
    </T> */
    fun <T> type(type: Class<T>, vararg componentTypes: Class<*>): Generator

    /**
     *
     * Gives a generator that can produce instances of the type of the
     * given reflected method parameter.
     *
     *
     * If the parameter is marked with an annotation that influences the
     * generation of its value, that annotation will be applied to the
     * generation of values for that parameter's type.
     *
     * @param parameter a reflected method parameter
     * @return generator that can produce values of the parameter's type
     */
    fun parameter(parameter: Parameter): Generator

    /**
     *
     * Gives a generator that can produce instances of the type of the
     * given reflected field.
     *
     *
     * If the field is marked with an annotation that influences the
     * generation of its value, that annotation will be applied to the
     * generation of values for that field's type.
     *
     * @param field a reflected field
     * @return generator that can produce values of the field's type
     */
    fun field(field: Field): Generator

    /**
     *
     * Makes a generator access point just like the receiver, but which
     * uses the given source of randomness for making random choices.
     *
     *
     * Intended for use by junit-quickcheck.
     *
     * @param random a source of randomness
     * @return a generator access point that has the source of randomness
     * available to it
     */
    fun withRandom(random: SourceOfRandomness): Generators
}