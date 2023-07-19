package examples.algorithms

import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.utbot.examples.algorithms.BinarySearch
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate

class SummaryBinarySearchTest : SummaryTestCaseGeneratorTest(
    BinarySearch::class,
) {
    @Test
    fun testLeftBinSearch() {
        val summary1 = "Test does not iterate while(left < right - 1), executes conditions:\n" +
                "    (found): False\n" +
                "returns from: return -1;\n"
        val summary2 = "Test iterates the loop while(left < right - 1) once,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    (array[middle] == key): False,\n" +
                "    (array[middle] < key): True\n" +
                "Test later executes conditions:\n" +
                "    (found): False\n" +
                "returns from: return -1;"
        val summary3 = "Test iterates the loop while(left < right - 1) once,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    (array[middle] == key): True,\n" +
                "    (array[middle] < key): False\n" +
                "Test later executes conditions:\n" +
                "    (found): True\n"  +
                "returns from: return right + 1;\n"
        val summary4 = "Test iterates the loop while(left < right - 1) once,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    (array[middle] == key): True,\n" +
                "    (array[middle] < key): False\n" +
                "Test afterwards executes conditions:\n" +
                "    (found): True\n" +
                "returns from: return right + 1;\n"
        val summary5 = "Test invokes:\n" +
                "    org.utbot.examples.algorithms.BinarySearch#isUnsorted(long[]) once\n" +
                "throws NullPointerException when: isUnsorted(array)\n"
        val summary6 = "Test invokes:\n" +
                "    org.utbot.examples.algorithms.BinarySearch#isUnsorted(long[]) once\n" +
                "executes conditions:\n" +
                "    (isUnsorted(array)): True\n" +
                "throws IllegalArgumentException when: isUnsorted(array)\n"

        val methodName1 = "testLeftBinSearch_NotFound"
        val methodName2 = "testLeftBinSearch_MiddleOfArrayLessThanKey"
        val methodName3 = "testLeftBinSearch_Found"
        val methodName4 = "testLeftBinSearch_Found_1"
        val methodName5 = "testLeftBinSearch_BinarySearchIsUnsorted"
        val methodName6 = "testLeftBinSearch_IsUnsorted"

        val displayName1 = "found : False -> return -1"
        val displayName2 = "array[middle] == key : False -> return -1"
        val displayName3 = "while(left < right - 1) -> return right + 1"
        val displayName4 = "while(left < right - 1) -> return right + 1"
        val displayName5 = "isUnsorted(array) -> ThrowNullPointerException"
        val displayName6 = "isUnsorted(array) -> ThrowIllegalArgumentException"

        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3,
            summary4,
            summary5,
            summary6
        )

        val displayNames = listOf(
            displayName1,
            displayName2,
            displayName3,
            displayName4,
            displayName5,
            displayName6
        )

        val methodNames = listOf(
            methodName1,
            methodName2,
            methodName3,
            methodName4,
            methodName5,
            methodName6
        )

        val method = BinarySearch::leftBinSearch
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}