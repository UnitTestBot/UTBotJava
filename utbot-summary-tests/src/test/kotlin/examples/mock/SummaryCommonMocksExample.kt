package examples.mock

import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.utbot.examples.mock.CommonMocksExample
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate

class SummaryCommonMocksExample : SummaryTestCaseGeneratorTest(
    CommonMocksExample::class,
) {
    @Test
    fun testClinitMockExample() {
        val summary1 = "Test invokes:\n" +
                "    {@link java.lang.Integer#intValue()} twice\n" +
                "returns from: return -ObjectWithFinalStatic.keyValue;\n"

        val methodName1 = "testClinitMockExample_IntegerIntValue"

        val displayName1 = "IntegerIntValue -> return -ObjectWithFinalStatic.keyValue"


        val summaryKeys = listOf(
            summary1
        )

        val displayNames = listOf(
            displayName1
        )

        val methodNames = listOf(
            methodName1
        )

        val method = CommonMocksExample::clinitMockExample
        val mockStrategy = MockStrategyApi.OTHER_CLASSES
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}