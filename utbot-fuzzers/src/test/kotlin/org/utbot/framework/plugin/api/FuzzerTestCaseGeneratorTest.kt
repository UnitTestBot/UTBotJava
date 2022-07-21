package org.utbot.framework.plugin.api

import org.utbot.fuzzer.primitive.PrimitiveFuzzer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

// TODO: no support for String
internal class FuzzerTestCaseGeneratorTest {
    @ParameterizedTest
    @MethodSource("manyMethods")
    fun testManyMethods(method: KFunction<*>, returnType: KClass<*>, vararg paramTypes: KClass<*>) {
        val valueTestSet = generate(method)
        valueTestSet.executions.forEach { execution ->
            assertEquals(paramTypes.toList(), execution.stateBefore.params.map { it.type }) { "$method" }
            assertTrue(execution.returnValue.isSuccess) { "$method" }
            val value = execution.returnValue.getOrNull()
            assertNotNull(value) { "$method" }
            assertEquals(returnType, value!!::class) { "$method" }
        }
    }

    companion object {
        /**
         * Arguments for generated types checks.
         *
         * Each line contains:
         * - method
         * - parameter types
         * - return type
         */
        @Suppress("unused")
        @JvmStatic
        fun manyMethods() = listOf(
            args(Object::equals, Any::class, returnType = Boolean::class),
            args(Object::hashCode, returnType = Int::class),
            args(Math::copySign, Double::class, Double::class, returnType = Double::class)
        )

        private fun args(method: KFunction<*>, vararg paramTypes: KClass<*>, returnType: KClass<*>) =
            arguments(method, returnType, paramTypes)
    }
}

private fun generate(method: KFunction<*>) =
    PrimitiveFuzzer.generate(UtMethod.from(method), MockStrategyApi.NO_MOCKS)