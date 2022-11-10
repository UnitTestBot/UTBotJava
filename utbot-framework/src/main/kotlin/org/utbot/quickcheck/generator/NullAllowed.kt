package org.utbot.quickcheck.generator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Mark a parameter of a {@link org.utbot.quickcheck.Property}
 * method with this annotation to indicate that the parameter is nullable, and
 * to optionally configure the probability of generating a null value.</p>
 */
@Target({ PARAMETER, FIELD, ANNOTATION_TYPE, TYPE_USE })
@Retention(RUNTIME)
public @interface NullAllowed {
    /**
     * @return probability of generating {@code null}, in the range [0,1]
     */
    double probability() default 0.2;
}
