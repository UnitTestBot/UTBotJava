package org.utbot.examples.mixed

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import org.utbot.framework.plugin.api.isNotNull
import org.utbot.framework.plugin.api.isNull
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.UtValueTestCaseChecker

internal class ImageIOUsageTest : UtValueTestCaseChecker(testClass = ImageIOUsage::class) {

    @Test
    fun testIsAlphaPremultiplied() {
        checkMocksAndInstrumentation(
            ImageIOUsage::isAlphaPremultiplied,
            eq(2),
            { _, _, instrumentation, _ -> theOnlyStaticMockValue(instrumentation).isNull() },
            { _, _, instrumentation, _ -> theOnlyStaticMockValue(instrumentation).isNotNull() },
            coverage = DoNotCalculate,
            mockStrategy = MockStrategyApi.OTHER_CLASSES
        )
    }

    private fun theOnlyStaticMockValue(instrumentation: List<UtInstrumentation>): UtModel =
        instrumentation
            .filterIsInstance<UtStaticMethodInstrumentation>()
            .single()
            .values
            .single()

}