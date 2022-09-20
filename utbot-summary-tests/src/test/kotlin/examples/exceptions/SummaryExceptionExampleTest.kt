package examples.exceptions

import examples.CustomJavaDocTagsEnabler
import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.examples.exceptions.ExceptionExamples
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.tests.infrastructure.DoNotCalculate

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
}