package org.utbot.quickcheck.generator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Mark a parameter of a
 * method with this annotation to constrain the values generated for the
 * parameter to a given range.</p>
 *
 * <p>Different generators may use different min/max attribute pairs.
 * Generators that produce primitive values or values of their wrapper types
 * will likely want to use the attribute pairs of corresponding type.
 * Otherwise, a generator can use {@link #min()} and {@link #max()}, and
 * take on the responsibility of converting their string values to values of
 * the desired type.</p>
 */
@Target({ PARAMETER, FIELD, ANNOTATION_TYPE, TYPE_USE })
@Retention(RUNTIME)
@GeneratorConfiguration
public @interface InRange {
    /**
     * @return a minimum {@code byte} value
     */
    byte minByte() default Byte.MIN_VALUE;

    /**
     * @return a maximum {@code byte} value
     */
    byte maxByte() default Byte.MAX_VALUE;

    /**
     * @return a minimum {@code short} value
     */
    short minShort() default Short.MIN_VALUE;

    /**
     * @return a maximum {@code short} value
     */
    short maxShort() default Short.MAX_VALUE;

    /**
     * @return a minimum {@code char} value
     */
    char minChar() default Character.MIN_VALUE;

    /**
     * @return a maximum {@code char} value
     */
    char maxChar() default Character.MAX_VALUE;

    /**
     * @return a minimum {@code int} value
     */
    int minInt() default Integer.MIN_VALUE;

    /**
     * @return a maximum {@code int} value
     */
    int maxInt() default Integer.MAX_VALUE;

    /**
     * @return a minimum {@code long} value
     */
    long minLong() default Long.MIN_VALUE;

    /**
     * @return a maximum {@code long} value
     */
    long maxLong() default Long.MAX_VALUE;

    /**
     * @return a minimum {@code float} value
     */
    float minFloat() default 0F;

    /**
     * @return a maximum {@code float} value
     */
    float maxFloat() default 1F;

    /**
     * @return a minimum {@code double} value
     */
    double minDouble() default 0D;

    /**
     * @return a maximum {@code double} value
     */
    double maxDouble() default 1D;

    /**
     * @return a minimum value, represented in string form
     */
    String min() default "";

    /**
     * @return a maximum value, represented in string form
     */
    String max() default "";

    /**
     * @return a formatting hint, such as a
     * {@linkplain java.text.SimpleDateFormat date format string}, that
     * generators can use when converting values from strings
     */
    String format() default "";
}
