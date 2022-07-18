package examples.inner

import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.inner.NestedCalls
import org.utbot.framework.plugin.api.MockStrategyApi

class SummaryNestedCallsTest : SummaryTestCaseGeneratorTest(
    NestedCalls::class,
) {
    @Test
    fun testInvokeExample() {
        val summary1 = "Test calls {@link org.utbot.examples.inner.NestedCalls.ExceptionExamples#initAnArray(int)},\n" +
                "    there it catches exception:\n" +
                "        IndexOutOfBoundsException e\n" +
                "    returns from: return -3;\n" +
                "    \n" +
                "Test next returns from: return exceptionExamples.initAnArray(n);\n"
        val summary2 = "Test calls {@link org.utbot.examples.inner.NestedCalls.ExceptionExamples#initAnArray(int)},\n" +
                "    there it catches exception:\n" +
                "        NegativeArraySizeException e\n" +
                "    returns from: return -2;\n" +
                "    \n" +
                "Test afterwards returns from: return exceptionExamples.initAnArray(n);"
        val summary3 = "Test calls {@link org.utbot.examples.inner.NestedCalls.ExceptionExamples#initAnArray(int)},\n" +
                "    there it returns from: return a[n - 1] + a[n - 2];\n" +
                "    \n" +
                "Test next returns from: return exceptionExamples.initAnArray(n);\n"

        val methodName1 = "testCallInitExamples_CatchIndexOutOfBoundsException"
        val methodName2 = "testCallInitExamples_CatchNegativeArraySizeException"
        val methodName3 = "testCallInitExamples_ReturnN1OfAPlusN2OfA"

        val displayName1 = "Catch (IndexOutOfBoundsException e) -> return -3"
        val displayName2 = "Catch (NegativeArraySizeException e) -> return -2"
        val displayName3 = "initAnArray -> return a[n - 1] + a[n - 2]"

        val method = NestedCalls::callInitExamples
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        check(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}