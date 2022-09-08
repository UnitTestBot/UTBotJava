package org.utbot.examples.mock

import org.utbot.tests.infrastructure.UtModelTestCaseChecker
import org.utbot.tests.infrastructure.primitiveValue
import org.utbot.framework.plugin.api.MockStrategyApi.OTHER_PACKAGES
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.getOrThrow
import org.utbot.framework.plugin.api.isMockModel
import org.utbot.framework.plugin.api.isNotNull
import org.utbot.framework.plugin.api.isNull
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class InnerMockWithFieldChecker : UtModelTestCaseChecker(testClass = InnerMockWithFieldExample::class) {
    @Test
    fun testCheckAndUpdate() {
        checkStatic(
            InnerMockWithFieldExample::checkAndUpdate,
            eq(4),
            { example, r -> example.isNull() && r.isException<NullPointerException>() },
            { example, r -> example.isNotNull() && example.stamp.isNull() && r.isException<NullPointerException>() },
            { example, r ->
                val result = r.getOrThrow()
                val isMockModels = example.stamp.isMockModel() && result.isMockModel()
                val stampConstraint = example.stamp.initial > example.stamp.version
                val postcondition = result.initial == example.stamp.initial && result.version == result.initial

                isMockModels && stampConstraint && postcondition
            },
            { example, r ->
                val result = r.getOrThrow()
                val stamp = example.stamp

                val isMockModels = stamp.isMockModel() && result.isMockModel()
                val stampConstraint = stamp.initial <= stamp.version
                val postcondition = result.initial == stamp.initial && result.version == stamp.version + 1

                isMockModels && stampConstraint && postcondition
            },
            mockStrategy = OTHER_PACKAGES
        )
    }

    private val UtModel.stamp: UtModel
        get() = findField("stamp")

    private val UtModel.initial: Int
        get() = findField("initial").primitiveValue()

    private val UtModel.version: Int
        get() = findField("version").primitiveValue()
}