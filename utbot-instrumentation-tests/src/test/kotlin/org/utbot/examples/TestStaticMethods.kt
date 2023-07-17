package org.utbot.examples

import org.utbot.examples.samples.staticenvironment.StaticExampleClass
import org.utbot.framework.plugin.api.util.signature
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.execute
import org.utbot.instrumentation.instrumentation.coverage.CoverageInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.collectCoverage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


class TestStaticMethods {
    lateinit var utContext: AutoCloseable

    @Test
    fun testStaticMethodCall() {
        ConcreteExecutor(
            CoverageInstrumentation.Factory(),
            StaticExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val res1 = it.execute(StaticExampleClass::inc, arrayOf())
            val coverageInfo1 = it.collectCoverage(StaticExampleClass::class.java)

            assertEquals(0, res1.getOrNull())
            assertEquals((3..6).toList() + (9..22).toList(), coverageInfo1.visitedInstrs)

            val res2 = it.execute(
                StaticExampleClass::plus,
                arrayOf(5)
            )
            val coverageInfo2 = it.collectCoverage(StaticExampleClass::class.java)

            assertEquals(0, res2.getOrNull())
            assertTrue(
                coverageInfo2.methodToInstrRange[StaticExampleClass::plus.signature]!!.toList()
                    .subtract(coverageInfo2.visitedInstrs)
                    .isEmpty()
            )
        }
    }

    @Test
    fun testNullableMethod() {
        ConcreteExecutor(
            CoverageInstrumentation.Factory(),
            StaticExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val res1 = it.execute(
                StaticExampleClass::canBeNull,
                arrayOf(10,
                "123")
            )
            assertEquals("123", res1.getOrNull())

            val res2 = it.execute(
                StaticExampleClass::canBeNull,
                arrayOf(0,
                "kek")
            )

            assertEquals(null, res2.getOrNull())

            val res3 = it.execute(
                StaticExampleClass::canBeNull,
                arrayOf(1,
                null)
            )

            assertEquals(null, res3.getOrNull())
        }
    }

    @Test
    fun testNullableMethodWithoutAnnotations() {
        ConcreteExecutor(
            CoverageInstrumentation.Factory(),
            StaticExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val res1 = it.execute(
                StaticExampleClass::canBeNullWithoutAnnotations,
                arrayOf(10,
                "123")
            )
            assertEquals("123", res1.getOrNull())

            val res2 = it.execute(
                StaticExampleClass::canBeNullWithoutAnnotations,
                arrayOf(0,
                "kek")
            )

            assertEquals(null, res2.getOrNull())

            val res3 = it.execute(
                StaticExampleClass::canBeNullWithoutAnnotations,
                arrayOf(1,
                null)
            )

            assertEquals(null, res3.getOrNull())
        }
    }
}