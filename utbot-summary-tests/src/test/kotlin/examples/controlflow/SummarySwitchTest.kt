package examples.controlflow

import examples.CustomJavaDocTagsEnabler
import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.examples.controlflow.Switch
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.tests.infrastructure.DoNotCalculate

@ExtendWith(CustomJavaDocTagsEnabler::class)
class SummarySwitchTest : SummaryTestCaseGeneratorTest(
    Switch::class
) {
    @Test
    fun testDifferentExceptions() {
        val summary1 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#simpleSwitch(int)}\n" +
                "@utbot.activatesSwitch {@code case 10}\n" +
                "@utbot.returnsFrom {@code return 10;}"
        val summary2 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#simpleSwitch(int)}\n" +
                "@utbot.activatesSwitch {@code case default}\n" +
                "@utbot.returnsFrom {@code return -1;}"
        val summary3 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#simpleSwitch(int)}\n" +
                "@utbot.activatesSwitch {@code case 12}\n" +
                "@utbot.returnsFrom {@code return 12;}"
        val summary4 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#simpleSwitch(int)}\n" +
                "@utbot.activatesSwitch {@code case 13}\n" +
                "@utbot.returnsFrom {@code return 13;}"

        val methodName1 = "testSimpleSwitch_Return10"
        val methodName2 = "testSimpleSwitch_ReturnNegative1"
        val methodName3 = "testSimpleSwitch_Return12"
        val methodName4 = "testSimpleSwitch_Return13"

        val displayName1 = "switch(x) case: 10 -> return 10"
        val displayName2 = "switch(x) case: Default -> return -1"
        val displayName3 = "switch(x) case: 12 -> return 12"
        val displayName4 = "switch(x) case: 13 -> return 13"

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

        val method = Switch::simpleSwitch
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}