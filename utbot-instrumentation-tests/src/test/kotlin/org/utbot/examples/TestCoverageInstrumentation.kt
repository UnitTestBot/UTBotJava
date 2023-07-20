package org.utbot.examples

import com.jetbrains.rd.util.reactive.RdFault
import org.utbot.examples.samples.ExampleClass
import org.utbot.examples.statics.substitution.StaticSubstitution
import org.utbot.examples.statics.substitution.StaticSubstitutionExamples
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.signature
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.execute
import org.utbot.instrumentation.instrumentation.coverage.CoverageInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.collectCoverage
import org.utbot.instrumentation.util.InstrumentedProcessError
import org.utbot.instrumentation.util.StaticEnvironment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TestCoverageInstrumentation {
    lateinit var utContext: AutoCloseable

    @Test
    fun testCatchTargetException() {
        ConcreteExecutor(
            CoverageInstrumentation.Factory,
            ExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val testObject = ExampleClass()

            val res = it.execute(ExampleClass::kek2, arrayOf(testObject, 123))
            val coverageInfo = it.collectCoverage(ExampleClass::class.java)

            assertEquals(5, coverageInfo.visitedInstrs.size)
            assertEquals(43..48, coverageInfo.methodToInstrRange[ExampleClass::kek2.signature])
            assertTrue(res.exceptionOrNull() is ArrayIndexOutOfBoundsException)
        }
    }

    @Test
    fun testIfBranches() {
        ConcreteExecutor(
            CoverageInstrumentation.Factory,
            ExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val testObject = ExampleClass()

            it.execute(ExampleClass::bar, arrayOf(testObject, 2))
            val coverageInfo1 = it.collectCoverage(ExampleClass::class.java)

            assertEquals(17, coverageInfo1.visitedInstrs.size)
            assertEquals(14..42, coverageInfo1.methodToInstrRange[ExampleClass::bar.signature])

            it.execute(ExampleClass::bar, arrayOf(testObject, 0))
            val coverageInfo2 = it.collectCoverage(ExampleClass::class.java)

            assertEquals(16, coverageInfo2.visitedInstrs.size)
            assertEquals(14..42, coverageInfo2.methodToInstrRange[ExampleClass::bar.signature])
        }
    }

    @Test
    fun testWrongArgumentsException() {
        ConcreteExecutor(
            CoverageInstrumentation.Factory,
            ExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val testObject = ExampleClass()
            val exc = assertThrows<InstrumentedProcessError> {
                it.execute(
                    ExampleClass::bar,
                    arrayOf(testObject, 1, 2, 3)
                )
            }

            assertInstanceOf(
                RdFault::class.java,
                exc.cause!!
            )
            assertTrue((exc.cause as RdFault).reasonTypeFqn == "IllegalArgumentException")
        }
    }


    @Test
    fun testMultipleRunsInsideCoverage() {
        ConcreteExecutor(
            CoverageInstrumentation.Factory,
            ExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val testObject = ExampleClass()
            val exc = assertThrows<InstrumentedProcessError> {
                it.execute(
                    ExampleClass::bar,
                    arrayOf(testObject, 1, 2, 3)
                )
            }

            assertInstanceOf(
                RdFault::class.java,
                exc.cause!!
            )
            assertTrue((exc.cause as RdFault).reasonTypeFqn == "IllegalArgumentException")

            it.execute(ExampleClass::bar, arrayOf(testObject, 2))
            val coverageInfo1 = it.collectCoverage(ExampleClass::class.java)

            assertEquals(17, coverageInfo1.visitedInstrs.size)
            assertEquals(14..42, coverageInfo1.methodToInstrRange[ExampleClass::bar.signature])

            it.execute(ExampleClass::bar, arrayOf(testObject, 0))
            val coverageInfo2 = it.collectCoverage(ExampleClass::class.java)

            assertEquals(16, coverageInfo2.visitedInstrs.size)
            assertEquals(14..42, coverageInfo2.methodToInstrRange[ExampleClass::bar.signature])
        }
    }


    @Test
    fun testSameResult() {
        ConcreteExecutor(
            CoverageInstrumentation.Factory,
            ExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val testObject = ExampleClass()

            it.execute(ExampleClass::dependsOnField, arrayOf(testObject))
            val coverageInfo1 = it.collectCoverage(ExampleClass::class.java)

            assertEquals(19, coverageInfo1.visitedInstrs.size)
            assertEquals(90..115, coverageInfo1.methodToInstrRange[ExampleClass::dependsOnField.signature])

            it.execute(ExampleClass::dependsOnField, arrayOf(testObject))
            val coverageInfo2 = it.collectCoverage(ExampleClass::class.java)

            assertEquals(19, coverageInfo2.visitedInstrs.size)
            assertEquals(90..115, coverageInfo2.methodToInstrRange[ExampleClass::dependsOnField.signature])
        }
    }

    @Test
    fun testResult() {
        ConcreteExecutor(
            CoverageInstrumentation.Factory,
            ExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val testObject = ExampleClass()

            val res = it.execute(ExampleClass::foo, arrayOf(testObject, 3))
            val coverageInfo = it.collectCoverage(ExampleClass::class.java)

            assertEquals(1, res.getOrNull())
            assertEquals(33, coverageInfo.visitedInstrs.size)
            assertEquals(49..89, coverageInfo.methodToInstrRange[ExampleClass::foo.signature])
        }
    }

    @Test
    fun testEmptyMethod() {
        ConcreteExecutor(
            CoverageInstrumentation.Factory,
            ExampleClass::class.java.protectionDomain.codeSource.location.path
        ).use {
            val testObject = ExampleClass()

            val res = it.execute(ExampleClass::emptyMethod, arrayOf(testObject))
            val coverageInfo = it.collectCoverage(ExampleClass::class.java)

            assertEquals(Unit::class, res.getOrNull()!!::class)
            assertEquals(1, coverageInfo.visitedInstrs.size)
        }
    }

    @Test
    fun testTernaryOperator() {
        ConcreteExecutor(
            CoverageInstrumentation.Factory,
            StaticSubstitutionExamples::class.java.protectionDomain.codeSource.location.path
        ).use {
            val testObject = StaticSubstitutionExamples()

            val emptyStaticEnvironment = StaticEnvironment()

            val res1 = it.execute(StaticSubstitutionExamples::lessThanZero, arrayOf(testObject), parameters = emptyStaticEnvironment)

            val staticEnvironment = StaticEnvironment(
                StaticSubstitution::mutableValue.fieldId to -1
            )
            val res2 = it.execute(StaticSubstitutionExamples::lessThanZero, arrayOf(testObject), parameters = staticEnvironment)
            val coverageInfo = it.collectCoverage(StaticSubstitutionExamples::class.java)

            assertEquals(res1.getOrNull(), 5)
            assertEquals(res2.getOrNull(), 0)
            assertEquals(coverageInfo.visitedInstrs, (3..10).toList())
        }
    }
}