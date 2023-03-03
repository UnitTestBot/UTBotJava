package math

import examples.CustomJavaDocTagsEnabler
import examples.SummaryTestCaseGeneratorTest
import guava.examples.math.IntMath
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtClusterInfo
import org.utbot.testing.DoNotCalculate

@ExtendWith(CustomJavaDocTagsEnabler::class)
class SummaryIntMathLogTest : SummaryTestCaseGeneratorTest(
    IntMath::class,
) {
    @Test
    fun testLog2() {
        val summary1 = "@utbot.classUnderTest {@link IntMath}\n" +
                "@utbot.methodUnderTest {@link guava.examples.math.IntMath#log2(int,java.math.RoundingMode)}\n"
        val summary2 = "@utbot.classUnderTest {@link IntMath}\n" +
                "@utbot.methodUnderTest {@link guava.examples.math.IntMath#log2(int,java.math.RoundingMode)}\n"
        val summary3 = "@utbot.classUnderTest {@link IntMath}\n" +
                "@utbot.methodUnderTest {@link guava.examples.math.IntMath#log2(int,java.math.RoundingMode)}\n"
        val summary4 = "@utbot.classUnderTest {@link IntMath}\n" +
                "@utbot.methodUnderTest {@link guava.examples.math.IntMath#log2(int,java.math.RoundingMode)}\n" +
                "@utbot.invokes {@link java.math.RoundingMode#ordinal()}\n" +
                "@utbot.throwsException {@link java.lang.NullPointerException} in: mode"

        val methodName1 = "testLog2_IntegerNumberOfLeadingZeros"
        val methodName2 = "testLog2_IntegerNumberOfLeadingZeros_1"
        val methodName3 = "testLog2_IntMathLessThanBranchFree"
        val methodName4 = "testLog2_RoundingModeOrdinal"

        val displayName1 = "switch(mode) case: FLOOR -> return (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(x)"
        val displayName2 = "switch(mode) case: CEILING -> return Integer.SIZE - Integer.numberOfLeadingZeros(x - 1)"
        val displayName3 = "switch(mode) case: HALF_EVEN -> return logFloor + lessThanBranchFree(cmp, x)"
        val displayName4 = "switch(mode) case:  -> ThrowNullPointerException"

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

        val clusterInfo = listOf(
            Pair(UtClusterInfo("SYMBOLIC EXECUTION: SUCCESSFUL EXECUTIONS for method log2(int, java.math.RoundingMode)", null), 3),
            Pair(UtClusterInfo("SYMBOLIC EXECUTION: ERROR SUITE for method log2(int, java.math.RoundingMode)", null), 1)
        )

        val method = IntMath::log2
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames, clusterInfo)
    }
}