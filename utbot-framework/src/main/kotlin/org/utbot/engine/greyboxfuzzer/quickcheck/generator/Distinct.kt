package org.utbot.engine.greyboxfuzzer.quickcheck.generator

/**
 *
 * Mark a parameter of a
 * method with this annotation to make values generated for the parameter
 * distinct from each other.
 *
 *
 * This annotation is recognized on array parameters and parameters of type
 * [java.util.Collection] and [java.util.Map].
 *
 *
 * Using this annotation with [Size] on [java.util.Set] or
 * [java.util.Map] leads to strict size constraint.
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@GeneratorConfiguration
annotation class Distinct
