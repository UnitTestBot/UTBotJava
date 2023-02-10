package examples.collections

import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.utbot.examples.collections.ListWrapperReturnsVoidExample
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate

/**
 * Tests verify that the previously discovered bug is not reproducible anymore.
 *
 * To get more details, see [issue-437](https://github.com/UnitTestBot/UTBotJava/issues/437)
 */
class SummaryListWrapperReturnsVoidTest : SummaryTestCaseGeneratorTest(
    ListWrapperReturnsVoidExample::class,
) {
    @Test
    fun testRunForEach() {
        val summary1 = "Test invokes:\n" +
                "    {@link java.util.List#forEach(java.util.function.Consumer)} once\n" +
                "throws NullPointerException in: list.forEach(o -> {\n" +
                "    if (o == null)\n" +
                "        i[0]++;\n" +
                "});\n"
        val summary2 = "Test returns from: return i[0];"
        val summary3 = "Test returns from: return i[0];"
        val summary4 = "Test returns from: return i[0];"

        val methodName1 = "testRunForEach_ListForEach"
        val methodName2 = "testRunForEach_Return0OfI"
        val methodName3 = "testRunForEach_Return0OfI_1"
        val methodName4 = "testRunForEach_Return0OfI_2"

        val displayName1 = "list.forEach(o -> { if (o == null) i[0]++ }) : True -> ThrowNullPointerException"
        val displayName2 = "-> return i[0]"
        val displayName3 = "-> return i[0]"
        val displayName4 = "-> return i[0]"

        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3,
            summary4
        )

        val displayNames = listOf(
            displayName1,
            displayName2,
            displayName3,
            displayName4
        )

        val methodNames = listOf(
            methodName1,
            methodName2,
            methodName3,
            methodName4
        )

        val method = ListWrapperReturnsVoidExample::runForEach
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testSumPositiveForEach() {
        val summary1 = "Test throws NullPointerException in: list.forEach(i -> {\n" +
                "    if (i > 0) {\n" +
                "        sum[0] += i;\n" +
                "    }\n" +
                "});"
        val summary2 = "Test invokes: {@link java.util.List#forEach(java.util.function.Consumer)} once\n" +
                "throws NullPointerException in: list.forEach(i -> {\n" +
                "    if (i > 0) {\n" +
                "        sum[0] += i;\n" +
                "    }\n" +
                "});"
        val summary3 = "Test executes conditions:\n" +
                "    (sum[0] == 0): True\n" +
                "returns from: return 0;"
        val summary4 = "Test executes conditions:\n" +
                "    (sum[0] == 0): True\n" +
                "returns from: return 0;"
        val summary5 = "Test executes conditions:\n" +
                "    (sum[0] == 0): False\n" +
                "returns from: return sum[0];"

        val methodName1 = "testSumPositiveForEach_ThrowNullPointerException"
        val methodName2 = "testSumPositiveForEach_ListForEach"
        val methodName3 = "testSumPositiveForEach_0OfSumEqualsZero"
        val methodName4 = "testSumPositiveForEach_0OfSumEqualsZero_1"
        val methodName5 = "testSumPositiveForEach_0OfSumNotEqualsZero"

        val displayName1 = "list.forEach(i -> { if (i > 0) { sum[0] += i } }) : True -> ThrowNullPointerException"
        val displayName2 = "list.forEach(i -> { if (i > 0) { sum[0] += i } }) : True -> ThrowNullPointerException"
        val displayName3 = "sum[0] == 0 : True -> return 0"
        val displayName4 = "sum[0] == 0 : True -> return 0"
        val displayName5 = "sum[0] == 0 : False -> return sum[0]"

        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3,
            summary4,
            summary5
        )

        val displayNames = listOf(
            displayName1,
            displayName2,
            displayName3,
            displayName4,
            displayName5
        )

        val methodNames = listOf(
            methodName1,
            methodName2,
            methodName3,
            methodName4,
            methodName5
        )

        val method = ListWrapperReturnsVoidExample::sumPositiveForEach
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}