package examples.controlflow

import examples.CustomJavaDocTagsEnabler
import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.examples.controlflow.Switch
import org.utbot.examples.exceptions.ExceptionExamples
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate

@ExtendWith(CustomJavaDocTagsEnabler::class)
class SummarySwitchTest : SummaryTestCaseGeneratorTest(
    Switch::class
) {
    @Test
    fun testSimpleSwitch() {
        val summary1 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#simpleSwitch(int)}\n" +
                "@utbot.activatesSwitch {@code switch(x) case: 10}\n" +
                "@utbot.returnsFrom {@code return 10;}"
        val summary2 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#simpleSwitch(int)}\n" +
                "@utbot.activatesSwitch {@code switch(x) case: default}\n" +
                "@utbot.returnsFrom {@code return -1;}"
        val summary3 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#simpleSwitch(int)}\n" +
                "@utbot.activatesSwitch {@code switch(x) case: 12}\n" +
                "@utbot.returnsFrom {@code return 12;}"
        val summary4 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#simpleSwitch(int)}\n" +
                "@utbot.activatesSwitch {@code switch(x) case: 13}\n" +
                "@utbot.returnsFrom {@code return 13;}"

        val methodName1 = "testSimpleSwitch_Return10"
        val methodName2 = "testSimpleSwitch_ReturnNegative1"
        val methodName3 = "testSimpleSwitch_Return12"
        val methodName4 = "testSimpleSwitch_Return13"

        val displayName1 = "switch(x) case: 10 -> return 10"
        val displayName2 = "switch(x) case: default -> return -1"
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

    @Test
    fun testCharToIntSwitch() {
        val summary1 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#charToIntSwitch(char)}\n" +
                "@utbot.activatesSwitch {@code switch(c) case: 'C'}\n" +
                "@utbot.returnsFrom {@code return 100;}\n"
        val summary2 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#charToIntSwitch(char)}\n" +
                "@utbot.activatesSwitch {@code switch(c) case: 'V'}\n" +
                "@utbot.returnsFrom {@code return 5;}\n"
        val summary3 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#charToIntSwitch(char)}\n" +
                "@utbot.activatesSwitch {@code switch(c) case: 'I'}\n" +
                "@utbot.returnsFrom {@code return 1;}\n"
        val summary4 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#charToIntSwitch(char)}\n" +
                "@utbot.activatesSwitch {@code switch(c) case: 'X'}\n" +
                "@utbot.returnsFrom {@code return 10;}\n"
        val summary5 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#charToIntSwitch(char)}\n" +
                "@utbot.activatesSwitch {@code switch(c) case: 'M'}\n" +
                "@utbot.returnsFrom {@code return 1000;}\n"
        val summary6 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#charToIntSwitch(char)}\n" +
                "@utbot.activatesSwitch {@code switch(c) case: 'D'}\n" +
                "@utbot.returnsFrom {@code return 500;}\n"
        val summary7 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#charToIntSwitch(char)}\n" +
                "@utbot.activatesSwitch {@code switch(c) case: 'L'}\n" +
                "@utbot.returnsFrom {@code return 50;}\n"
        val summary8 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#charToIntSwitch(char)}\n" +
                "@utbot.invokes {@link java.lang.StringBuilder#append(java.lang.String)}\n" +
                "@utbot.invokes {@link java.lang.StringBuilder#append(char)}\n" +
                "@utbot.invokes {@link java.lang.StringBuilder#toString()}\n" +
                "@utbot.activatesSwitch {@code switch(c) case: default}\n" +
                "@utbot.throwsException {@link java.lang.IllegalArgumentException} when: switch(c) case: default\n"

        val methodName1 = "testCharToIntSwitch_Return100"
        val methodName2 = "testCharToIntSwitch_Return5"
        val methodName3 = "testCharToIntSwitch_Return1"
        val methodName4 = "testCharToIntSwitch_Return10"
        val methodName5 = "testCharToIntSwitch_Return1000"
        val methodName6 = "testCharToIntSwitch_Return500"
        val methodName7 = "testCharToIntSwitch_Return50"
        val methodName8 = "testCharToIntSwitch_ThrowIllegalArgumentException"

        val displayName1 = "switch(c) case: 'C' -> return 100"
        val displayName2 = "switch(c) case: 'V' -> return 5"
        val displayName3 = "switch(c) case: 'I' -> return 1"
        val displayName4 = "switch(c) case: 'X' -> return 10"
        val displayName5 = "switch(c) case: 'M' -> return 1000"
        val displayName6 = "switch(c) case: 'D' -> return 500"
        val displayName7 = "switch(c) case: 'L' -> return 50"
        val displayName8 = "switch(c) case: default -> ThrowIllegalArgumentException"

        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3,
            summary4,
            summary5,
            summary6,
            summary7,
            summary8,
        )

        val displayNames = listOf(
            displayName1,
            displayName2,
            displayName3,
            displayName4,
            displayName5,
            displayName6,
            displayName7,
            displayName8,
        )

        val methodNames = listOf(
            methodName1,
            methodName2,
            methodName3,
            methodName4,
            methodName5,
            methodName6,
            methodName7,
            methodName8,
        )

        val method = Switch::charToIntSwitch
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testThrowExceptionInSwitchArgument() {
        val summary1 = "@utbot.classUnderTest {@link Switch}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Switch#throwExceptionInSwitchArgument()}\n" +
                "@utbot.invokes org.utbot.examples.controlflow.Switch#getChar()\n" +
                "@utbot.throwsException {@link java.lang.RuntimeException} in: switch(getChar())\n"

        val methodName1 = "testThrowExceptionInSwitchArgument_ThrowRuntimeException"

        val displayName1 = "switch(getChar()) -> ThrowRuntimeException"

        val summaryKeys = listOf(
            summary1,
        )

        val displayNames = listOf(
            displayName1,
        )

        val methodNames = listOf(
            methodName1,
        )

        val method = Switch::throwExceptionInSwitchArgument
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}
