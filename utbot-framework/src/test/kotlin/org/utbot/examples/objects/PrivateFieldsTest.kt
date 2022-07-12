package org.utbot.examples.objects

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.eq
import org.utbot.examples.isException
import org.junit.jupiter.api.Test

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