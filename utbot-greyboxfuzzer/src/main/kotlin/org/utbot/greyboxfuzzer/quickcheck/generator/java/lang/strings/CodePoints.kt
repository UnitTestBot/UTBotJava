package org.utbot.greyboxfuzzer.quickcheck.generator.java.lang.strings

import java.nio.charset.Charset
import java.nio.charset.CharsetEncoder

/**
 * Maps ordinal values to corresponding Unicode code points in a
 * [Charset].
 */
class CodePoints internal constructor() {
    private val ranges: MutableList<CodePointRange>

    init {
        ranges = ArrayList()
    }

    /**
     * @param index index to look up
     * @return this code point set's `index`'th code point
     * @throws IndexOutOfBoundsException if there is no such code point
     */
    fun at(index: Int): Int {
        if (index < 0) {
            throw IndexOutOfBoundsException(
                "illegal negative index: $index"
            )
        }
        var min = 0
        var max = ranges.size - 1
        while (min <= max) {
            val midpoint = min + (max - min) / 2
            val current = ranges[midpoint]
            if (index >= current.previousCount
                && index < current.previousCount + current.size()
            ) {
                return current.low + index - current.previousCount
            } else if (index < current.previousCount) {
                max = midpoint - 1
            } else {
                min = midpoint + 1
            }
        }
        throw IndexOutOfBoundsException(index.toString())
    }

    /**
     * @return how many code points are in this code point set
     */
    fun size(): Int {
        if (ranges.isEmpty()) return 0
        val last = ranges[ranges.size - 1]
        return last.previousCount + last.size()
    }

    /**
     * @param codePoint a code point
     * @return whether this code point set contains the given code point
     */
    operator fun contains(codePoint: Int): Boolean {
        return ranges.stream().anyMatch { r: CodePointRange -> r.contains(codePoint) }
    }

    fun add(range: CodePointRange) {
        ranges.add(range)
    }

    class CodePointRange(low: Int, high: Int, previousCount: Int) {
        val low: Int
        val high: Int
        val previousCount: Int

        init {
            require(low <= high) { String.format("%d > %d", low, high) }
            this.low = low
            this.high = high
            this.previousCount = previousCount
        }

        operator fun contains(codePoint: Int): Boolean {
            return codePoint >= low && codePoint <= high
        }

        fun size(): Int {
            return high - low + 1
        }
    }

    companion object {
        private val ENCODABLES: MutableMap<Charset, CodePoints> = HashMap()

        /**
         * Gives a set of the code points in the given charset.
         *
         * @param c a charset
         * @return the set of code points in the charset
         */
        @JvmStatic
        fun forCharset(c: Charset): CodePoints? {
            if (ENCODABLES.containsKey(c)) return ENCODABLES[c]
            val points = load(c)
            ENCODABLES[c] = points
            return points
        }

        private fun load(c: Charset): CodePoints {
            require(c.canEncode()) { "Charset " + c.name() + " does not support encoding" }
            return encodableCodePoints(c.newEncoder())
        }

        private fun encodableCodePoints(encoder: CharsetEncoder): CodePoints {
            val points = CodePoints()
            var start = 0
            var inRange = false
            var current = 0
            var previousCount = 0
            val buffer = IntArray(1)
            while (current <= Character.MAX_CODE_POINT) {
                encoder.reset()
                buffer[0] = current
                val s = String(buffer, 0, 1)
                if (encoder.canEncode(s)) {
                    if (!inRange) {
                        inRange = true
                        start = current
                    }
                } else if (inRange) {
                    inRange = false
                    val range = CodePointRange(start, current - 1, previousCount)
                    points.add(range)
                    previousCount += range.size()
                }
                ++current
            }
            if (inRange) {
                points.add(
                    CodePointRange(start, current - 1, previousCount)
                )
            }
            return points
        }
    }
}