package org.utbot.engine.greyboxfuzzer.quickcheck.internal

import org.utbot.engine.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.math.BigInteger

object Ranges {
    enum class Type(val pattern: String) {
        CHARACTER("c"), INTEGRAL("d"), FLOAT("f"), STRING("s");
    }

    fun <T : Comparable<T>?> checkRange(type: Type, min: T, max: T): Int {
        val comparison = min!!.compareTo(max)
        require(comparison <= 0) {
            String.format(
                "bad range, %" + type.pattern + " > %" + type.pattern,
                min,
                max
            )
        }
        return comparison
    }

    fun choose(
        random: SourceOfRandomness,
        min: BigInteger?,
        max: BigInteger
    ): BigInteger {
        val range = max.subtract(min).add(BigInteger.ONE)
        var generated: BigInteger
        do {
            generated = random.nextBigInteger(range.bitLength())
        } while (generated.compareTo(range) >= 0)
        return generated.add(min)
    }

    fun choose(random: SourceOfRandomness, min: Long, max: Long): Long {
        checkRange(Type.INTEGRAL, min, max)

        /* There are some edges cases with integer overflows, for instance,
       when (max - min) exceeds Long.MAX_VALUE. These cases should be
       relatively rare under the assumption that choosing
       [Long.MIN_VALUE, Long.MAX_VALUE] can be simplified to choosing any
       random long. Thus, the optimization here only deals with the common
       situation that no overflows are possible (maybe the heuristic to
       detect that could be improved).
     */
        val noOverflowIssues = max < 1L shl 62 && min > -(1L shl 62)
        return if (noOverflowIssues) {
            // fast path: use long computations
            val range = max - min + 1
            val mask = findNextPowerOfTwoLong(range) - 1

            // loop to avoid distribution bias (as would be the case
            // with modulo division)
            var generated: Long
            do {
                generated = Math.abs(random.nextLong()) and mask
            } while (generated >= range)
            generated + min
        } else {
            // slow path: fall back to BigInteger to avoid any surprises
            choose(
                random,
                BigInteger.valueOf(min),
                BigInteger.valueOf(max)
            )
                .toLong()
        }
    }

    fun findNextPowerOfTwoLong(positiveLong: Long): Long {
        return if (isPowerOfTwoLong(positiveLong)) positiveLong else 1L shl 64 - java.lang.Long.numberOfLeadingZeros(
            positiveLong
        )
    }

    private fun isPowerOfTwoLong(positiveLong: Long): Boolean {
        return positiveLong and positiveLong - 1 == 0L
    }
}