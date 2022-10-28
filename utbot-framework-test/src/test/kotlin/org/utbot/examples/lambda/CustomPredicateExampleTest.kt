package org.utbot.examples.lambda

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withConcrete
import org.utbot.tests.infrastructure.CodeGeneration
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.isException

class CustomPredicateExampleTest : UtValueTestCaseChecker(
    testClass = CustomPredicateExample::class,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        // TODO: https://github.com/UnitTestBot/UTBotJava/issues/88 (generics in Kotlin)
        // At the moment, when we create an instance of a functional interface via lambda (through reflection),
        // we need to do a type cast (e.g. `obj as Predicate<Int>`), but since generics are not supported yet,
        // we use a raw type (e.g. `Predicate`) instead (which is not allowed in Kotlin).
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
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
        checkStatics(
            CustomPredicateExample::capturedStaticFieldPredicateCheck,
            eq(3),
            { predicate, x, statics, r ->
                val staticField = statics.values.singleOrNull()?.value as Int
                CustomPredicateExample.someStaticField = staticField

                !predicate.test(x) && r == false
            },
            { predicate, x, statics, r ->
                val staticField = statics.values.singleOrNull()?.value as Int
                CustomPredicateExample.someStaticField = staticField

                predicate.test(x) && r == true
            },
            { predicate, _, _, _ ->
                predicate == null
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testCapturedNonStaticFieldPredicateCheck() {
        // TODO fails without concrete https://github.com/UnitTestBot/UTBotJava/issues/1247
        withConcrete(useConcreteExecution = true) {
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
}
