package org.utbot.examples.objects

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.isException
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