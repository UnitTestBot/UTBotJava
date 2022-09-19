package org.utbot.examples.objects

import org.utbot.tests.infrastructure.Full
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.isException
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

class AnonymousClassesExampleTest : UtValueTestCaseChecker(testClass = AnonymousClassesExample::class) {
    @Test
    fun testAnonymousClassAsParam() {
        checkWithException(
            AnonymousClassesExample::anonymousClassAsParam,
            eq(3),
            { abstractAnonymousClass, r -> abstractAnonymousClass == null && r.isException<NullPointerException>() },
            { abstractAnonymousClass, r -> abstractAnonymousClass != null && r.getOrNull() == 0 },
            { abstractAnonymousClass, r -> abstractAnonymousClass != null && abstractAnonymousClass::class.java.isAnonymousClass && r.getOrNull() == 42 },
            coverage = Full
        )
    }

    @Test
    fun testNonFinalAnonymousStatic() {
        checkStaticsAndException(
            AnonymousClassesExample::nonFinalAnonymousStatic,
            eq(3),
            { statics, r -> statics.values.single().value == null && r.isException<NullPointerException>() },
            { _, r -> r.getOrNull() == 0 },
            { _, r -> r.getOrNull() == 42 },
            coverage = Full
        )
    }

    @Test
    fun testAnonymousClassAsStatic() {
        check(
            AnonymousClassesExample::anonymousClassAsStatic,
            eq(1),
            { r -> r == 42 },
            coverage = Full
        )
    }

    @Test
    fun testAnonymousClassAsResult() {
        check(
            AnonymousClassesExample::anonymousClassAsResult,
            eq(1),
            { abstractAnonymousClass -> abstractAnonymousClass != null && abstractAnonymousClass::class.java.isAnonymousClass },
            coverage = Full
        )
    }
}