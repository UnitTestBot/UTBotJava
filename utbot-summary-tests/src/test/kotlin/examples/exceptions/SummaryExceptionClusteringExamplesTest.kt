package examples.exceptions

import examples.CustomJavaDocTagsEnabler
import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.testing.DoNotCalculate
import org.utbot.examples.exceptions.ExceptionClusteringExamples
import org.utbot.framework.plugin.api.MockStrategyApi

@ExtendWith(CustomJavaDocTagsEnabler::class)
class SummaryExceptionClusteringExamplesTest : SummaryTestCaseGeneratorTest(
    ExceptionClusteringExamples::class
) {
    @Test
    fun testDifferentExceptions() {
        val summary1 = "@utbot.classUnderTest {@link ExceptionClusteringExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.exceptions.ExceptionClusteringExamples#differentExceptions(int)}\n" +
                "@utbot.executesCondition {@code (i == 0): True}\n" +
                "@utbot.throwsException {@link java.lang.ArithmeticException} in: return 100 / i;"

        val summary2 = "@utbot.classUnderTest {@link ExceptionClusteringExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.exceptions.ExceptionClusteringExamples#differentExceptions(int)}\n" +
                "@utbot.executesCondition {@code (i == 0): False}\n" +
                "@utbot.executesCondition {@code (i == 1): True}\n" +
                "@utbot.throwsException {@link org.utbot.examples.exceptions.MyCheckedException} when: i == 1"
        val summary3 = "@utbot.classUnderTest {@link ExceptionClusteringExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.exceptions.ExceptionClusteringExamples#differentExceptions(int)}\n" +
                "@utbot.executesCondition {@code (i == 0): False}\n" +
                "@utbot.executesCondition {@code (i == 1): False}\n" +
                "@utbot.executesCondition {@code (i == 2): True}\n" +
                "@utbot.throwsException {@link java.lang.IllegalArgumentException} when: i == 2"
        val summary4 = "@utbot.classUnderTest {@link ExceptionClusteringExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.exceptions.ExceptionClusteringExamples#differentExceptions(int)}\n" +
                "@utbot.executesCondition {@code (i == 0): False}\n" +
                "@utbot.executesCondition {@code (i == 1): False}\n" +
                "@utbot.executesCondition {@code (i == 2): False}\n" +
                "@utbot.returnsFrom {@code return i * 2;}\n"

        val methodName1 = "testDifferentExceptions_IEqualsZero"
        val methodName2 = "testDifferentExceptions_IEquals1"
        val methodName3 = "testDifferentExceptions_IEquals2"
        val methodName4 = "testDifferentExceptions_INotEquals2"

        val displayName1 = "return 100 / i : True -> ThrowArithmeticException"
        val displayName2 = "i == 1 -> ThrowMyCheckedException"
        val displayName3 = "i == 2 -> ThrowIllegalArgumentException"
        val displayName4 = "i == 0 : False -> return i * 2"

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

        val method = ExceptionClusteringExamples::differentExceptions
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}