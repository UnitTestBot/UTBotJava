
package org.utbot.quickcheck.generator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>Apply this annotation to an annotation that marks property parameters,
 * in order that the marked annotation can be used to configure
 * {@linkplain Generator generators} for values of the parameter's type.</p>
 *
 * <p>If a generator has a public instance method named {@code configure},
 * with a single parameter whose type is an annotation that has this annotation
 * applied, then when a property that has a parameter marked with method that
 * annotation is verified, the generator that generates the value for that
 * parameter will have its {@code configure} method called with the annotation
 * as the argument.</p>
 */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Documented
public @interface GeneratorConfiguration {
}
