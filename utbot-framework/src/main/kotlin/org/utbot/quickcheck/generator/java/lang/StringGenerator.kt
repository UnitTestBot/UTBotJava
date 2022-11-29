package org.utbot.quickcheck.generator.java.lang

import org.utbot.quickcheck.random.SourceOfRandomness
import kotlin.random.Random

/**
 *
 * Produces [String]s whose characters are in the interval
 * `[0x0000, 0xD7FF]`.
 */
class StringGenerator : AbstractStringGenerator() {

    private var codePoints = setOf(0 until Character.MIN_SURROGATE.code)
    override var lengthRange: IntRange? = null

    override fun nextCodePoint(random: SourceOfRandomness): Int {
        return codePoints.random().random()
        val codePoint = chooseRandomCodePoint()
        return codePoint.random()
    }

    private fun chooseRandomCodePoint(): IntRange {
//        val size = codePoints.sumOf { it.last - it.first }
//        val randomIntToSize = Random.nextInt(size)
//        var curSum = 0
//        for (codePoint in codePoints) {
//            val codePointSize = codePoint.last - codePoint.first
//            curSum += codePointSize
//            if (curSum >= randomIntToSize) {
//                return codePoint
//            }
//        }
        return codePoints.random()
    }
    fun configure(codePoints: Set<IntRange>, lengthRange: IntRange) {
        this.codePoints = codePoints
        this.lengthRange = lengthRange
    }
}