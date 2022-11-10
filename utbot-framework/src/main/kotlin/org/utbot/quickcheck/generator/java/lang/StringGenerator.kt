package org.utbot.quickcheck.generator.java.lang

import org.utbot.quickcheck.random.SourceOfRandomness

/**
 *
 * Produces [String]s whose characters are in the interval
 * `[0x0000, 0xD7FF]`.
 */
class StringGenerator : AbstractStringGenerator() {
    override fun nextCodePoint(random: SourceOfRandomness): Int {
        return random.nextInt(0, Character.MIN_SURROGATE.code - 1)
    }
}