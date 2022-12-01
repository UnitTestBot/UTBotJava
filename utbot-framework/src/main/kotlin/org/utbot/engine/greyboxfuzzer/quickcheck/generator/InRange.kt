package org.utbot.engine.greyboxfuzzer.quickcheck.generator

/**
 *
 * Mark a parameter of a
 * method with this annotation to constrain the values generated for the
 * parameter to a given range.
 *
 *
 * Different generators may use different min/max attribute pairs.
 * Generators that produce primitive values or values of their wrapper types
 * will likely want to use the attribute pairs of corresponding type.
 * Otherwise, a generator can use [.min] and [.max], and
 * take on the responsibility of converting their string values to values of
 * the desired type.
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@GeneratorConfiguration
annotation class InRange(
    /**
     * @return a minimum `byte` value
     */
    val minByte: Byte = Byte.MIN_VALUE,
    /**
     * @return a maximum `byte` value
     */
    val maxByte: Byte = Byte.MAX_VALUE,
    /**
     * @return a minimum `short` value
     */
    val minShort: Short = Short.MIN_VALUE,
    /**
     * @return a maximum `short` value
     */
    val maxShort: Short = Short.MAX_VALUE,
    /**
     * @return a minimum `char` value
     */
    val minChar: Char = Character.MIN_VALUE,
    /**
     * @return a maximum `char` value
     */
    val maxChar: Char = Character.MAX_VALUE,
    /**
     * @return a minimum `int` value
     */
    val minInt: Int = Int.MIN_VALUE,
    /**
     * @return a maximum `int` value
     */
    val maxInt: Int = Int.MAX_VALUE,
    /**
     * @return a minimum `long` value
     */
    val minLong: Long = Long.MIN_VALUE,
    /**
     * @return a maximum `long` value
     */
    val maxLong: Long = Long.MAX_VALUE,
    /**
     * @return a minimum `float` value
     */
    val minFloat: Float = 0f,
    /**
     * @return a maximum `float` value
     */
    val maxFloat: Float = 1f,
    /**
     * @return a minimum `double` value
     */
    val minDouble: Double = 0.0,
    /**
     * @return a maximum `double` value
     */
    val maxDouble: Double = 1.0,
    /**
     * @return a minimum value, represented in string form
     */
    val min: String = "",
    /**
     * @return a maximum value, represented in string form
     */
    val max: String = "",
    /**
     * @return a formatting hint, such as a
     * [date format string][java.text.SimpleDateFormat], that
     * generators can use when converting values from strings
     */
    val format: String = ""
)