package org.utbot.examples.objects

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.isException
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

class AnonymousClassesExampleTest : UtValueTestCaseChecker(testClass = AnonymousClassesExample::class) {
    @Test
    fun testAnonymousClassAsParam() {
        checkWithException(
            AnonymousClassesExample::anonymousClassAsParam,
            eq(2),
            { abstractAnonymousClass, r -> abstractAnonymousClass == null && r.isException<NullPointerException>() },
            { abstractAnonymousClass, r -> abstractAnonymousClass != null && r.getOrNull() == 0 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testNonFinalAnonymousStatic() {
        check(
            AnonymousClassesExample::nonFinalAnonymousStatic,
            eq(0), // we remove all anonymous classes in statics
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testAnonymousClassAsStatic() {
        check(
            AnonymousClassesExample::anonymousClassAsStatic,
            eq(0), // we remove all anonymous classes in statics
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testAnonymousClassAsResult() {
        check(
            AnonymousClassesExample::anonymousClassAsResult,
            eq(0), // we remove anonymous classes from the params and the result
            coverage = DoNotCalculate
        )
    }
}