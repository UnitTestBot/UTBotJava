package org.utbot.examples.codegen.modifiers

import org.junit.jupiter.api.Test
import org.utbot.common.withAccessibility
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jField
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.UtValueTestCaseChecker

class ClassWithPrivateMutableFieldOfPrivateTypeTest : UtValueTestCaseChecker(
    testClass = ClassWithPrivateMutableFieldOfPrivateType::class
) {
    @Test
    fun testChangePrivateMutableFieldWithPrivateType() {
        checkAllMutationsWithThis(
            ClassWithPrivateMutableFieldOfPrivateType::changePrivateMutableFieldWithPrivateType,
            eq(1),
            { thisBefore, _, thisAfter, _, r ->
                val privateMutableField = FieldId(
                    ClassWithPrivateMutableFieldOfPrivateType::class.id,
                    "privateMutableField"
                ).jField

                val (privateFieldBeforeValue, privateFieldAfterValue) = privateMutableField.withAccessibility {
                    privateMutableField.get(thisBefore) to privateMutableField.get(thisAfter)
                }

                privateFieldBeforeValue == null && privateFieldAfterValue != null && r == 0
            }
        )
    }
}
