package org.utbot.examples.unsafe

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

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