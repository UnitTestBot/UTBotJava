package org.utbot.examples.mock

import org.utbot.framework.plugin.api.MockStrategyApi.OTHER_PACKAGES
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.isNull
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testing.UtModelTestCaseChecker
import org.utbot.testing.getOrThrow
import org.utbot.testing.primitiveValue

internal class MockWithFieldChecker : UtModelTestCaseChecker(testClass = MockWithFieldExample::class) {
    @Test
    fun testCheckAndUpdate() {
        check(
            MockWithFieldExample::checkAndUpdate,
            eq(3),
            { stamp, r -> stamp.isNull() && r.isException<NullPointerException>() },
            { stamp, r ->
                val result = r.getOrThrow()

                val mockModels = stamp.isMockModel() && result.isMockModel()
                val stampValues = stamp.initial > stamp.version
                val resultConstraint = result.initial == stamp.initial && result.version == result.initial

                mockModels && stampValues && resultConstraint
            },
            { stamp, r ->
                val result = r.getOrThrow()

                val mockModels = stamp.isMockModel() && result.isMockModel()
                val stampValues = stamp.initial <= stamp.version
                val resultConstraint = result.initial == stamp.initial && result.version == stamp.version + 1

                mockModels && stampValues && resultConstraint
            },
            mockStrategy = OTHER_PACKAGES
        )
    }

    private val UtModel.initial: Int
        get() = findField("initial").primitiveValue()

    private val UtModel.version: Int
        get() = findField("version").primitiveValue()
}