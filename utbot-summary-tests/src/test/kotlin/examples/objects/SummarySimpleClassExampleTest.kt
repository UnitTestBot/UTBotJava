package examples.objects

import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.utbot.examples.objects.SimpleClassExample
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate

class SummarySimpleClassExampleTest : SummaryTestCaseGeneratorTest(
    SimpleClassExample::class,
) {
    @Test
    fun testImmutableFieldAccess() {
        val summary1 = "Test \n" +
                "throws NullPointerException when: c.b == 10\n"
        val summary2 = "Test executes conditions:\n" +
                "    (c.b == 10): False\n" +
                "returns from: return 1;\n"
        val summary3 = "Test executes conditions:\n" +
                "    (c.b == 10): True\n" +
                "returns from: return 0;\n"

        val methodName1 = "testImmutableFieldAccess_ThrowNullPointerException"
        val methodName2 = "testImmutableFieldAccess_CBNotEquals10"
        val methodName3 = "testImmutableFieldAccess_CBEquals10"

        val displayName1 = "c.b == 10 -> ThrowNullPointerException"
        val displayName2 = "c.b == 10 : False -> return 1"
        val displayName3 = "c.b == 10 : True -> return 0"

        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3
        )

        val displayNames = listOf(
            displayName1,
            displayName2,
            displayName3
        )

        val methodNames = listOf(
            methodName1,
            methodName2,
            methodName3
        )

        val method = SimpleClassExample::immutableFieldAccess
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}