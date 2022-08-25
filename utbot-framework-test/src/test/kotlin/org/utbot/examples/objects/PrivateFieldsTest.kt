package org.utbot.examples.objects

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.isException
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class PrivateFieldsTest : UtValueTestCaseChecker(testClass = PrivateFields::class) {
    @Test
    fun testAccessWithGetter() {
        checkWithException(
            PrivateFields::accessWithGetter,
            eq(3),
            { x, r -> x == null && r.isException<NullPointerException>() },
            { x, r -> x.a == 1 && r.getOrNull() == true },
            { x, r -> x.a != 1 && r.getOrNull() == false },
        )
    }
}