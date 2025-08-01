package org.utbot.examples

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.utbot.examples.samples.ExampleClass
import org.utbot.examples.samples.et.ClassSimpleCatch
import org.utbot.examples.statics.substitution.StaticSubstitution
import org.utbot.examples.statics.substitution.StaticSubstitutionExamples
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.signature
import org.utbot.instrumentation.execute
import org.utbot.instrumentation.instrumentation.coverage.BranchCoverageInstrumentation
import org.utbot.instrumentation.instrumentation.coverage.collectCoverage
import org.utbot.instrumentation.util.StaticEnvironment
import org.utbot.instrumentation.withInstrumentation

class TestBranchCoverageInstrumentation {
    lateinit var utContext: AutoCloseable

    @Test
    fun testIfBranches() {
        withInstrumentation(
            BranchCoverageInstrumentation.Factory,
            ExampleClass::class.java.protectionDomain.codeSource.location.path
        ) { executor ->
            val testObject = ExampleClass()

            executor.execute(ExampleClass::bar, arrayOf(testObject, 2))
            val coverageInfo1 = executor.collectCoverage(ExampleClass::class.java)

            assertEquals(2, coverageInfo1.visitedInstrs.size)
            assertEquals(1..3, coverageInfo1.methodToInstrRange[ExampleClass::bar.signature])

            executor.execute(ExampleClass::bar, arrayOf(testObject, 0))
            val coverageInfo2 = executor.collectCoverage(ExampleClass::class.java)

            assertEquals(2, coverageInfo2.visitedInstrs.size)
            assertEquals(1..3, coverageInfo2.methodToInstrRange[ExampleClass::bar.signature])
        }
    }

    @Test
    fun testTryCatch() {
        withInstrumentation(
            BranchCoverageInstrumentation.Factory,
            ClassSimpleCatch::class.java.protectionDomain.codeSource.location.path
        ) { executor ->
            executor.execute(ClassSimpleCatch::A_catches, emptyArray())
            val coverageInfo1 = executor.collectCoverage(ClassSimpleCatch::class.java)

            assertEquals(2, coverageInfo1.visitedInstrs.size)
            assertEquals(3..5, coverageInfo1.methodToInstrRange[ClassSimpleCatch::A_catches.signature])

            executor.execute(ClassSimpleCatch::A_catchesWrongException, emptyArray())
            val coverageInfo2 = executor.collectCoverage(ClassSimpleCatch::class.java)

            assertEquals(0, coverageInfo2.visitedInstrs.size)
            assertEquals(7..9, coverageInfo2.methodToInstrRange[ClassSimpleCatch::A_catchesWrongException.signature])
        }
    }

    @Test
    fun testTernaryOperator() {
        withInstrumentation(
            BranchCoverageInstrumentation.Factory,
            StaticSubstitutionExamples::class.java.protectionDomain.codeSource.location.path
        ) { executor ->
            val testObject = StaticSubstitutionExamples()

            val emptyStaticEnvironment = StaticEnvironment()

            val res1 = executor.execute(
                StaticSubstitutionExamples::lessThanZero,
                arrayOf(testObject),
                parameters = emptyStaticEnvironment
            )

            val staticEnvironment = StaticEnvironment(
                StaticSubstitution::mutableValue.fieldId to -1
            )
            val res2 = executor.execute(
                StaticSubstitutionExamples::lessThanZero,
                arrayOf(testObject),
                parameters = staticEnvironment
            )
            val coverageInfo = executor.collectCoverage(StaticSubstitutionExamples::class.java)

            assertEquals(res1.getOrNull(), 5)
            assertEquals(res2.getOrNull(), 0)
            assertEquals(coverageInfo.visitedInstrs, (1..3).toList())
        }
    }
}