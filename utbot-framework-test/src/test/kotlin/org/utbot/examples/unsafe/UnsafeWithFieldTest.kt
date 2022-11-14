package org.utbot.examples.unsafe

import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseChecker

internal class UnsafeWithFieldTest: UtValueTestCaseChecker(UnsafeWithField::class) {

    @Test
    fun checkSetField() {
        check(
            UnsafeWithField::setField,
            eq(1)
            // TODO JIRA:1579
            // for now we might have any value as a result, so there is no need in the matcher
        )
    }
}