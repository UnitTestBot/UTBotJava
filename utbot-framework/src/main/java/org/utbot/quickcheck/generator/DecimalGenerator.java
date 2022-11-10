
package org.utbot.quickcheck.generator;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Collections.singletonList;

/**
 * Base class for generators of decimal types, such as {@code double} and
 * {@link BigDecimal}. All numbers are converted to/from BigDecimal for
 * processing.
 *
 * @param <T> type of values this generator produces
 */
public abstract class DecimalGenerator<T extends Number> extends Generator<T> {
    protected DecimalGenerator(Class<T> type) {
        super(singletonList(type));
    }

    protected DecimalGenerator(List<Class<T>> types) {
        super(types);
    }

    /**
     * @return a predicate checking whether its input is in the configured
     * range
     */
    protected abstract Predicate<T> inRange();

}
