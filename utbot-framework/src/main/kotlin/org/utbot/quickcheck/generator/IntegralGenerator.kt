package org.utbot.quickcheck.generator;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Collections.singletonList;

/**
 * Base class for generators of integral types, such as {@code int} and
 * {@link BigInteger}. All numbers are converted to/from BigInteger for
 * processing.
 *
 * @param <T> type of values this generator produces
 */
public abstract class IntegralGenerator<T extends Number>
    extends Generator<T> {

    protected IntegralGenerator(Class<T> type) {
        super(singletonList(type));
    }

    protected IntegralGenerator(List<Class<T>> types) {
        super(types);
    }

    /**
     * @return a predicate checking whether its input is in the configured
     * range
     */
    protected abstract Predicate<T> inRange();

}
