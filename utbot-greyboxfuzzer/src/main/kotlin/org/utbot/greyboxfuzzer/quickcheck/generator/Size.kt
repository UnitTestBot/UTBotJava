package org.utbot.greyboxfuzzer.quickcheck.generator

/**
 *
 * Mark a parameter of a
 * method with this annotation to constrain the size of values generated for
 * the parameter.
 *
 *
 * This annotation is recognized on array parameters and parameters of type
 * [Collection][java.util.Collection.size] and [ ][java.util.Map.size].
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@GeneratorConfiguration
annotation class Size(val min: Int = 0, val max: Int)