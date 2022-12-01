package org.utbot.engine.greyboxfuzzer.quickcheck.generator

/**
 *
 * Mark a parameter of a
 * method with this annotation to constrain the values generated for the
 * parameter to a given precision.
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@GeneratorConfiguration
annotation class Precision(
    /**
     * @return desired [scale][java.math.BigDecimal.scale] of the
     * generated values
     */
    val scale: Int
)