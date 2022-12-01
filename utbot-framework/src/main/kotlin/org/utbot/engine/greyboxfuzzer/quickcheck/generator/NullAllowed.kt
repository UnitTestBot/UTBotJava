package org.utbot.engine.greyboxfuzzer.quickcheck.generator

/**
 *
 * Mark a parameter of a
 * method with this annotation to indicate that the parameter is nullable, and
 * to optionally configure the probability of generating a null value.
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class NullAllowed(
    /**
     * @return probability of generating `null`, in the range [0,1]
     */
    val probability: Double = 0.2
)