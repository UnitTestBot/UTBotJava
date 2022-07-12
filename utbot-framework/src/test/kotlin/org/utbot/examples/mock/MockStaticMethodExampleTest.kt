package org.utbot.examples.mock

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.util.singleModel
import org.utbot.framework.util.singleStaticMethod
import org.utbot.framework.util.singleValue
import org.junit.jupiter.api.Test

internal class MockStaticMethodExampleTest : UtValueTestCaseChecker(testClass = MockStaticMethodExample::class) {
    @Test
    fun testUseStaticMethod() {
        checkMocksAndInstrumentation(
            MockStaticMethodExample::useStaticMethod,
            eq(2),
            { _, instrumentation, r ->
                val mockValue = instrumentation
                    .singleStaticMethod("nextRandomInt")
                    .singleModel<UtPrimitiveModel>()
                    .singleValue() as Int

                mockValue > 50 && r == 100
            },
            { _, instrumentation, r ->
                val mockValue = instrumentation
                    .singleStaticMethod("nextRandomInt")
                    .singleModel<UtPrimitiveModel>()
                    .singleValue() as Int

                mockValue <= 50 && r == 0
            },
            coverage = DoNotCalculate,
            mockStrategy = MockStrategyApi.OTHER_PACKAGES
        )
    }
}