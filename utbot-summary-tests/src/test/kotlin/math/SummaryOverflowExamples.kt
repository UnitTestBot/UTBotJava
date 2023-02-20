package math

import examples.CustomJavaDocTagsEnabler
import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.examples.math.OverflowExamples
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testcheckers.withTreatingOverflowAsError
import org.utbot.testing.DoNotCalculate

@ExtendWith(CustomJavaDocTagsEnabler::class)
class SummaryOverflowExamples : SummaryTestCaseGeneratorTest(
    OverflowExamples::class
) {
    @Test
    fun testShortMulOverflow() {
        val summary1 = "@utbot.classUnderTest {@link OverflowExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.math.OverflowExamples#shortMulOverflow(short,short)}\n" +
                "@utbot.returnsFrom {@code return (short) (x * y);}\n"
        val summary2 = "@utbot.classUnderTest {@link OverflowExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.math.OverflowExamples#shortMulOverflow(short,short)}\n" +
                "@utbot.detectsSuspiciousBehavior in: return (short) (x * y);\n"

        val methodName1 = "testShortMulOverflow_ReturnXy"
        val methodName2 = "testShortMulOverflow_DetectOverflow"

        val displayName1 = "-> return (short) (x * y)"
        val displayName2 = "return (short) (x * y) : True -> DetectOverflow"

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

        val method = OverflowExamples::shortMulOverflow
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        withTreatingOverflowAsError {
            summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
        }
    }
}