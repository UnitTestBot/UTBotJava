package org.utbot.quickcheck.generator;

import org.utbot.quickcheck.random.SourceOfRandomness;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

/**
 * An access point for available generators.
 */
public interface Generators {

    /**
     * <p>Gives a generator that can produce values of the given type,
     * parameterized by the given "component" types.</p>
     *
     * @param <T> type of objects produced by the resulting generator
     * @param type a type
     * @param componentTypes types for the "components" of the type, if any
     * @return generator that can produce values of that type
     * @see ComponentizedGenerator
     */
    <T> Generator<T> type(Class<T> type, Class<?>... componentTypes);

    /**
     * <p>Gives a generator that can produce instances of the type of the
     * given reflected method parameter.</p>
     *
     * <p>If the parameter is marked with an annotation that influences the
     * generation of its value, that annotation will be applied to the
     * generation of values for that parameter's type.</p>
     *
     * @param parameter a reflected method parameter
     * @return generator that can produce values of the parameter's type
     */
    Generator<?> parameter(Parameter parameter);

    /**
     * <p>Gives a generator that can produce instances of the type of the
     * given reflected field.</p>
     *
     * <p>If the field is marked with an annotation that influences the
     * generation of its value, that annotation will be applied to the
     * generation of values for that field's type.</p>
     *
     * @param field a reflected field
     * @return generator that can produce values of the field's type
     */
    Generator<?> field(Field field);

    /**
     * <p>Makes a generator access point just like the receiver, but which
     * uses the given source of randomness for making random choices.</p>
     *
     * <p>Intended for use by junit-quickcheck.</p>
     *
     * @param random a source of randomness
     * @return a generator access point that has the source of randomness
     * available to it
     */
    Generators withRandom(SourceOfRandomness random);
}
