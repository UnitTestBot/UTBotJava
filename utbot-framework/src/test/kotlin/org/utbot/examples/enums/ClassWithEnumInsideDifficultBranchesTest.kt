package org.utbot.examples.enums

import org.utbot.examples.AbstractTestCaseGeneratorTest
import org.utbot.examples.eq
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ClassWithEnumInsideDifficultBranchesTest : AbstractTestCaseGeneratorTest(testClass = ClassWithEnumInsideDifficultBranches::class) {
    @Test
    @Disabled("TODO JIRA:1612")
    fun testDifficultIfBranch() {
        check(
            ClassWithEnumInsideDifficultBranches::useEnumInDifficultIf,
            eq(2),
            { s, r -> s.equals("TRYIF", ignoreCase = true) && r == 1 },
            { s, r -> !s.equals("TRYIF", ignoreCase = true) && r == 2 }
        )
    }
}