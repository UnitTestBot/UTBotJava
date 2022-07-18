package org.utbot.examples.objects

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.utbot.examples.isException
import org.junit.jupiter.api.Test

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