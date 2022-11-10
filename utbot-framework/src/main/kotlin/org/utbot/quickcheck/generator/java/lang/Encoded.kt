package org.utbot.quickcheck.generator.java.lang

import org.utbot.quickcheck.generator.GeneratorConfiguration
import org.utbot.quickcheck.generator.java.lang.strings.CodePoints
import org.utbot.quickcheck.generator.java.lang.strings.CodePoints.Companion.forCharset
import org.utbot.quickcheck.random.SourceOfRandomness
import java.nio.charset.Charset

/**
 *
 * Produces [String]s whose code points correspond to code points in
 * a given [Charset]
 * ([by default][Charset.defaultCharset]).
 */
class Encoded : AbstractStringGenerator() {
    private var charsetPoints: CodePoints? = null

    init {
        initialize(Charset.defaultCharset())
    }

    /**
     * Tells this generator to emit strings in the given charset.
     *
     * @param charset a charset to use as the source for characters of
     * generated strings
     */
    fun configure(charset: InCharset) {
        initialize(Charset.forName(charset.value))
    }

    private fun initialize(charset: Charset) {
        charsetPoints = forCharset(charset)
    }

    override fun nextCodePoint(random: SourceOfRandomness): Int {
        return charsetPoints!!.at(random.nextInt(0, charsetPoints!!.size() - 1))
    }

    /**
     * Names a [Charset].
     */
    @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.TYPE)
    @Retention(AnnotationRetention.RUNTIME)
    @GeneratorConfiguration
    annotation class InCharset(val value: String)
}