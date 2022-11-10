package org.utbot.quickcheck.generator;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Mark a parameter of a
 * method with this annotation to constrain the size of values generated for
 * the parameter.</p>
 *
 * <p>This annotation is recognized on array parameters and parameters of type
 * {@link java.util.Collection#size() Collection} and {@link
 * java.util.Map#size() Map}.</p>
 */
@Target({ PARAMETER, FIELD, ANNOTATION_TYPE, TYPE_USE })
@Retention(RUNTIME)
@GeneratorConfiguration
public @interface Size {
    int min() default 0;

    int max();
}
