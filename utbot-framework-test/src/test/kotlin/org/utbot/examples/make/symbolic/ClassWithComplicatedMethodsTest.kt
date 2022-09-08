package org.utbot.examples.make.symbolic

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockStrategyApi
import kotlin.math.abs
import kotlin.math.sqrt
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withoutConcrete
import org.utbot.tests.infrastructure.Compilation

// This class is substituted with ComplicatedMethodsSubstitutionsStorage
// but we cannot do in code generation.
// For this reason code generation executions are disabled
internal class ClassWithComplicatedMethodsTest : UtValueTestCaseChecker(
    testClass = ClassWithComplicatedMethods::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA, Compilation),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, Compilation)
    )
) {
    @Test
    @Disabled("[SAT-1419]")
    fun testApplyMethodWithSideEffectAndReturn() {
        checkMocksAndInstrumentation(
            ClassWithComplicatedMethods::applyMethodWithSideEffectAndReturn,
            eq(2),
            { x, mocks, instr, r ->
                x > 0 && mocks.isEmpty() && instr.isEmpty() && sqrt(x.toDouble()) == x.toDouble() && r!!.a == 2821
            },
            { x, mocks, instr, r ->
                x > 0 && mocks.isEmpty() && instr.isEmpty() && sqrt(x.toDouble()) != x.toDouble() && r!!.a == 10
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCreateWithOriginalConstructor() {
        checkMocksAndInstrumentation(
            ClassWithComplicatedMethods::createWithOriginalConstructor,
            eq(1),
            { a, b, mocks, instr, r -> a > 10 && b > 10 && r!!.a == a + b && mocks.isEmpty() && instr.isEmpty() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCreateWithSubstitutedConstructor() {
        withoutConcrete { // TODO: concrete execution can't handle this
            checkMocksAndInstrumentation(
                ClassWithComplicatedMethods::createWithSubstitutedConstructor,
                eq(1),
                { a, b, mocks, instr, r -> a < 0 && b < 0 && r!!.a == (a + b).toInt() && mocks.isEmpty() && instr.isEmpty() },
                coverage = DoNotCalculate
            )
        }
    }

    @Test
    fun testSqrt2() {
        checkMocksAndInstrumentation(
            ClassWithComplicatedMethods::sqrt2,
            eq(1),
            { mocks, instr, r -> abs(r!! - sqrt(2.0)) < eps && mocks.isEmpty() && instr.isEmpty() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testReturnSubstitutedMethod() {
        withoutConcrete { // TODO: concrete execution can't handle this
            checkMocksAndInstrumentation(
                ClassWithComplicatedMethods::returnSubstitutedMethod,
                eq(1),
                { x, mocks, instr, r -> x > 100 && mocks.isEmpty() && instr.isEmpty() && r != null && r.a == x },
                coverage = DoNotCalculate
            )
        }
    }

    @Test
    fun testAssumesWithMocks() {
        checkMocksAndInstrumentation(
            ClassWithComplicatedMethods::assumesWithMocks,
            eq(1),
            { x, mocks, instr, r -> x in 6..7 && r == 1 && mocks.isEmpty() && instr.isEmpty() },
            coverage = DoNotCalculate,
            mockStrategy = MockStrategyApi.OTHER_CLASSES
        )
    }

    private val eps = 1e-8
}