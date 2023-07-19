package examples.algorithms

import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.utbot.examples.algorithms.Sort
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate

class SummarySortTest : SummaryTestCaseGeneratorTest(
    Sort::class,
) {
    @Test
    fun testDefaultSort() {
        val summary1 = "Test \n" +
                "throws NullPointerException when: array.length < 4\n"
        val summary2 = "Test executes conditions:\n" +
                "    (array.length < 4): True\n" +
                "throws IllegalArgumentException when: array.length < 4\n"
        val summary3 = "Test executes conditions:\n" +
                "    (array.length < 4): False\n" +
                "invokes:\n" +
                "    {@link java.util.Arrays#sort(int[])} once\n" +
                "returns from: return array;\n"

        val methodName1 = "testDefaultSort_ThrowNullPointerException"
        val methodName2 = "testDefaultSort_ArrayLengthLessThan4"
        val methodName3 = "testDefaultSort_ArrayLengthGreaterOrEqual4"

        val displayName1 = "array.length < 4 -> ThrowNullPointerException"
        val displayName2 = "array.length < 4 -> ThrowIllegalArgumentException"
        val displayName3 = "array.length < 4 : False -> return array"

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

        val method = Sort::defaultSort
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}