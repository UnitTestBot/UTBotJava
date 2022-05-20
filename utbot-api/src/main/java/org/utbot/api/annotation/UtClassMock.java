package org.utbot.api.annotation;

/**
 * If internalUsage is true, there will be no mocks generated for the methods of the
 * class marked with this annotation in the resulting tests. Method substitutions will be used for the
 * analysis only.
 *
 * Otherwise, such mocks will be produced.
 *
 * @see UtInternalUsage
 */
public @interface UtClassMock {
    Class<?> target();
    boolean internalUsage() default false;
}
