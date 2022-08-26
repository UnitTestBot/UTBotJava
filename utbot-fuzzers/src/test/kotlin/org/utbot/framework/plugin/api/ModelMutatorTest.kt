package org.utbot.framework.plugin.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.utbot.fuzzer.invertBit
import kotlin.random.Random

class ModelMutatorTest {

    @Test
    fun `invert bit works for long`() {
        var attempts = 100_000
        val random = Random(2210)
        sequence {
            while (true) {
                yield(random.nextLong())
            }
        }.forEach { value ->
            if (attempts-- <= 0) { return }
            for (bit in 0 until Long.SIZE_BITS) {
                val newValue = value.invertBit(bit)
                val oldBinary = value.toBinaryString()
                val newBinary = newValue.toBinaryString()
                assertEquals(oldBinary.length, newBinary.length)
                for (test in Long.SIZE_BITS - 1 downTo 0) {
                    if (test != Long.SIZE_BITS - 1 - bit) {
                        assertEquals(oldBinary[test], newBinary[test]) { "$oldBinary : $newBinary for value $value" }
                    } else {
                        assertNotEquals(oldBinary[test], newBinary[test]) { "$oldBinary : $newBinary for value $value" }
                    }
                }
            }
        }
    }

    private fun Long.toBinaryString() = java.lang.Long.toBinaryString(this).padStart(Long.SIZE_BITS, '0')
}