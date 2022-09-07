package org.utbot.framework.concrete.constructors

import java.util.Optional
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.OptionalLong
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OptionalConstructorTest : BaseConstructorTest() {
    @Test
    fun testOptionalInt() {
        val optionalInt = OptionalInt.of(42)

        val reconstructed = computeReconstructed(optionalInt)

        assertEquals(optionalInt, reconstructed)
    }

    @Test
    fun testOptionalDouble() {
        val optionalDouble = OptionalDouble.of(42.0)

        val reconstructed = computeReconstructed(optionalDouble)

        assertEquals(optionalDouble, reconstructed)
    }

    @Test
    fun testOptionalLong() {
        val optionalLong = OptionalLong.of(42L)

        val reconstructed = computeReconstructed(optionalLong)

        assertEquals(optionalLong, reconstructed)
    }

    @Test
    fun testOptional() {
        val optional = Optional.of("42")

        val reconstructed = computeReconstructed(optional)

        assertEquals(optional, reconstructed)
    }

    @Test
    fun testRecursiveOptional() {
        val optional = Optional.of("42")
        val recursiveOptional = optional.map { optional }

        val reconstructed = computeReconstructed(recursiveOptional)

        assertEquals(reconstructed.get().get(), "42")
    }

    @Test
    fun testEmptyOptional() {
        val emptyOptional = Optional.empty<Int>()

        val reconstructed = computeReconstructed(emptyOptional)

        assertEquals(emptyOptional, reconstructed)
    }
}