package org.utbot.quickcheck.generator

/**
 *
 * Apply this annotation to an annotation that marks property parameters,
 * in order that the marked annotation can be used to configure
 * [generators][Generator] for values of the parameter's type.
 *
 *
 * If a generator has a public instance method named `configure`,
 * with a single parameter whose type is an annotation that has this annotation
 * applied, then when a property that has a parameter marked with method that
 * annotation is verified, the generator that generates the value for that
 * parameter will have its `configure` method called with the annotation
 * as the argument.
 */
@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class GeneratorConfiguration
