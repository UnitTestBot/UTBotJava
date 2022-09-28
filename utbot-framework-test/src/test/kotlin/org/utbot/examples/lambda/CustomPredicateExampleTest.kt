package org.utbot.examples.lambda

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.isException

class CustomPredicateExampleTest : UtValueTestCaseChecker(
    testClass = CustomPredicateExample::class,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        // TODO: https://github.com/UnitTestBot/UTBotJava/issues/88 (generics in Kotlin)
        // At the moment, when we create an instance of a functional interface via lambda (through reflection),
        // we need to do a type cast (e.g. `obj as Predicate<Int>`), but since generics are not supported yet,
        // we use a raw type (e.g. `Predicate`) instead (which is not allowed in Kotlin).
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testNoCapturedValuesPredicateCheck() {
        checkWithException(
            CustomPredicateExample::noCapturedValuesPredicateCheck,
            eq(3),
            { predicate, x, r -> !predicate.test(x) && r.getOrNull() == false },
            { predicate, x, r -> predicate.test(x) && r.getOrNull() == true },
            { predicate, _, r -> predicate == null && r.isException<NullPointerException>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCapturedLocalVariablePredicateCheck() {
        checkWithException(
            CustomPredicateExample::capturedLocalVariablePredicateCheck,
            eq(3),
            { predicate, x, r -> !predicate.test(x) && r.getOrNull() == false },
            { predicate, x, r -> predicate.test(x) && r.getOrNull() == true },
            { predicate, _, r -> predicate == null && r.isException<NullPointerException>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCapturedParameterPredicateCheck() {
        checkWithException(
            CustomPredicateExample::capturedParameterPredicateCheck,
            eq(3),
            { predicate, x, r -> !predicate.test(x) && r.getOrNull() == false },
            { predicate, x, r -> predicate.test(x) && r.getOrNull() == true },
            { predicate, _, r -> predicate == null && r.isException<NullPointerException>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCapturedStaticFieldPredicateCheck() {
        checkWithException(
            CustomPredicateExample::capturedStaticFieldPredicateCheck,
            eq(3),
            { predicate, x, r -> !predicate.test(x) && r.getOrNull() == false },
            { predicate, x, r -> predicate.test(x) && r.getOrNull() == true },
            { predicate, _, r -> predicate == null && r.isException<NullPointerException>() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCapturedNonStaticFieldPredicateCheck() {
        checkWithException(
            CustomPredicateExample::capturedNonStaticFieldPredicateCheck,
            eq(3),
            { predicate, x, r -> !predicate.test(x) && r.getOrNull() == false },
            { predicate, x, r -> predicate.test(x) && r.getOrNull() == true },
            { predicate, _, r -> predicate == null && r.isException<NullPointerException>() },
            coverage = DoNotCalculate
        )
    }
}
