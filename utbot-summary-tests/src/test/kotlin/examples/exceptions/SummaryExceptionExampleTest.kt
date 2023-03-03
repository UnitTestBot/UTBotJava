package examples.exceptions

import examples.CustomJavaDocTagsEnabler
import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.examples.exceptions.ExceptionExamples
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate

@ExtendWith(CustomJavaDocTagsEnabler::class)
class SummaryExceptionExampleTest : SummaryTestCaseGeneratorTest(
    ExceptionExamples::class
) {
    @Test
    fun testDifferentExceptions() {
        val summary1 = "@utbot.classUnderTest {@link ExceptionExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.exceptions.ExceptionExamples#nestedExceptions(int)}\n" +
                "@utbot.returnsFrom {@code return checkAll(i);}"
        val summary2 = "@utbot.classUnderTest {@link ExceptionExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.exceptions.ExceptionExamples#nestedExceptions(int)}\n" +
                "@utbot.returnsFrom {@code return -100;}\n" +
                "@utbot.caughtException {@code RuntimeException e}"
        val summary3 = "@utbot.classUnderTest {@link ExceptionExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.exceptions.ExceptionExamples#nestedExceptions(int)}\n" +
                "@utbot.returnsFrom {@code return 100;}\n" +
                "@utbot.caughtException {@code NullPointerException e}"

        val methodName1 = "testNestedExceptions_ReturnCheckAll"
        val methodName2 = "testNestedExceptions_CatchRuntimeException"
        val methodName3 = "testNestedExceptions_CatchNullPointerException"

        val displayName1 = "-> return checkAll(i)"
        val displayName2 = "Catch (RuntimeException e) -> return -100"
        val displayName3 = "Catch (NullPointerException e) -> return 100"

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

        val method = ExceptionExamples::nestedExceptions
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testHangForSeconds() {
        val summary1 = "@utbot.classUnderTest {@link ExceptionExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.exceptions.ExceptionExamples#hangForSeconds(int)}\n" +
                "@utbot.returnsFrom {@code return seconds;}\n"
        val summary2 = "@utbot.classUnderTest {@link ExceptionExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.exceptions.ExceptionExamples#hangForSeconds(int)}\n" +
                "@utbot.iterates iterate the loop {@code for(int i = 0; i < seconds; i++)} once\n" +
                "@utbot.returnsFrom {@code return seconds;}\n" +
                "@utbot.detectsSuspiciousBehavior in: return seconds;\n"

        val methodName1 = "testHangForSeconds_ReturnSeconds"
        val methodName2 = "testHangForSeconds_ThreadSleep"

        val displayName1 = "-> return seconds"
        val displayName2 = "return seconds -> TimeoutExceeded"

        val summaryKeys = listOf(
            summary1,
            summary2
        )

        val displayNames = listOf(
            displayName1,
            displayName2
        )

        val methodNames = listOf(
            methodName1,
            methodName2
        )

        val method = ExceptionExamples::hangForSeconds
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}