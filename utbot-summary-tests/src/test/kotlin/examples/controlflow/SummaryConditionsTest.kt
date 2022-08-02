package examples.controlflow

import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.controlflow.Conditions
import org.utbot.framework.plugin.api.MockStrategyApi

class SummaryConditionsTest : SummaryTestCaseGeneratorTest(
    Conditions::class
) {
    @Test
    fun testSimpleCondition() {
        val summary1 = "@utbot.classUnderTest {@link Conditions}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Conditions#simpleCondition(boolean)}\n" +
                "@utbot.executesCondition {@code (condition): False}\n" +
                "@utbot.returnsFrom {@code return 0;}"

        val summary2 = "@utbot.classUnderTest {@link Conditions}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Conditions#simpleCondition(boolean)}\n" +
                "@utbot.executesCondition {@code (condition): True}\n" +
                "@utbot.returnsFrom {@code return 1;}"

        val methodName1 = "testSimpleCondition_NotCondition"
        val methodName2 = "testSimpleCondition_Condition"

        val displayName1 = "condition : False -> return 0"
        val displayName2 = "condition : True -> return 1"

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

        val method = Conditions::simpleCondition
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        checkSummariesWithCustomTags(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}