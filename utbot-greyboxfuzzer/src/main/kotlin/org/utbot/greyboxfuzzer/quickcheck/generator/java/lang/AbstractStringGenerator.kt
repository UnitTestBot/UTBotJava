package org.utbot.greyboxfuzzer.quickcheck.generator.java.lang

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.generator.Size
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness

/**
 *
 * Base class for generators of values of type [String].
 *
 *
 * The generated values will have [String.length] decided by
 * [GenerationStatus.size].
 */
abstract class AbstractStringGenerator : Generator(String::class.java) {
    protected open var lengthRange: IntRange? = null

    override fun generate(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): UtModel {
        return generatorContext.utModelConstructor.construct(generateValue(random, status), stringClassId)
    }

    fun generateValue(
        random: SourceOfRandomness,
        status: GenerationStatus
    ): String {
        val codePoints = IntArray(lengthRange?.randomOrNull() ?: status.size())
        for (i in codePoints.indices) codePoints[i] = nextCodePoint(random)
        return String(codePoints, 0, codePoints.size)
    }

    protected abstract fun nextCodePoint(random: SourceOfRandomness): Int
}