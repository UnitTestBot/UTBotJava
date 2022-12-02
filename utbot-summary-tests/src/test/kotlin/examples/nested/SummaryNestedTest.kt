package examples.nested

import examples.CustomJavaDocTagsEnabler
import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.examples.nested.DeepNested
import org.utbot.examples.recursion.Recursion
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate

@ExtendWith(CustomJavaDocTagsEnabler::class)
class SummaryNestedTest : SummaryTestCaseGeneratorTest(
    DeepNested.Nested1.Nested2::class
) {
    @Test
    fun testNested() {
        val summary1 = "@utbot.classUnderTest {@link DeepNested.Nested1.Nested2}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.nested.DeepNested.Nested1.Nested2#f(int)}\n" +
                "@utbot.executesCondition {@code (i > 0): False}\n" +
                "@utbot.returnsFrom {@code return 0;}\n"

        val summary2 = "@utbot.classUnderTest {@link DeepNested.Nested1.Nested2}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.nested.DeepNested.Nested1.Nested2#f(int)}\n" +
                "@utbot.executesCondition {@code (i > 0): True}\n" +
                "@utbot.returnsFrom {@code return 10;}"

        val methodName1 = "testF_ILessOrEqualZero"
        val methodName2 = "testF_IGreaterThanZero"

        val displayName1 = "i > 0 : False -> return 0"
        val displayName2 = "i > 0 : True -> return 10"

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

        val method = DeepNested.Nested1.Nested2::f
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}