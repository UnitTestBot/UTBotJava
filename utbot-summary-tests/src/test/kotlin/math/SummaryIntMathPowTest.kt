package math

import examples.SummaryTestCaseGeneratorTest
import guava.examples.math.IntMath
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtClusterInfo
import org.utbot.testing.DoNotCalculate
class SummaryIntMathPowTest : SummaryTestCaseGeneratorTest(
    IntMath::class,
) {
    @Test
    fun testPow() {
        val summary1 = "Test activates switch case: 1, returns from: return 1;\n"
        val summary2 = "Test executes conditions:\n" +
                "    (k < Integer.SIZE): False\n" +
                "returns from: return 0;\n"
        val summary3 = "Test executes conditions:\n" +
                "    ((k < Integer.SIZE)): False\n" +
                "returns from: return (k < Integer.SIZE) ? (1 << k) : 0;\n"
        val summary4 = "Test iterates the loop for(int accum = 1; ; k >>= 1) once,\n" +
                "    inside this loop, the test returns from: return b * accum;"
        val summary5 = "Test executes conditions:\n" +
                "    ((k < Integer.SIZE)): True\n" +
                "returns from: return (k < Integer.SIZE) ? (1 << k) : 0;\n"
        val summary6 = "Test executes conditions:\n" +
                "    ((k == 0)): False\n" +
                "returns from: return (k == 0) ? 1 : 0;\n"
        val summary7 = "Test iterates the loop for(int accum = 1; ; k >>= 1) once,\n" +
                "    inside this loop, the test returns from: return accum;"
        val summary8 = "Test executes conditions:\n" +
                "    ((k == 0)): True\n" +
                "returns from: return (k == 0) ? 1 : 0;\n"
        val summary9 = "Test executes conditions:\n" +
                "    (k < Integer.SIZE): True,\n" +
                "    (((k & 1) == 0)): True\n" +
                "returns from: return ((k & 1) == 0) ? (1 << k) : -(1 << k);\n"
        val summary10 = "Test executes conditions:\n" +
                "    (k < Integer.SIZE): True,\n" +
                "    (((k & 1) == 0)): False\n" +
                "returns from: return ((k & 1) == 0) ? (1 << k) : -(1 << k);\n"
        val summary11 = "Test executes conditions:\n" +
                "    (((k & 1) == 0)): False\n" +
                "returns from: return ((k & 1) == 0) ? 1 : -1;\n"
        val summary12 = "Test executes conditions:\n" +
                "    (((k & 1) == 0)): True\n" +
                "returns from: return ((k & 1) == 0) ? 1 : -1;\n"
        val summary13 = "Test iterates the loop for(int accum = 1; ; k >>= 1) twice,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    (((k & 1) == 0)): False\n" +
                "returns from: return b * accum;"
        val summmary14 = "Test iterates the loop for(int accum = 1; ; k >>= 1) twice,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    (((k & 1) == 0)): True\n" +
                "returns from: return b * accum;"

        val methodName1 = "testPow_Return1"
        val methodName2 = "testPow_KGreaterOrEqualIntegerSIZE"
        val methodName3 = "testPow_KGreaterOrEqualIntegerSIZE_1"
        val methodName4 = "testPow_ReturnBMultiplyAccum"
        val methodName5 = "testPow_KLessThanIntegerSIZE"
        val methodName6 = "testPow_KNotEqualsZero"
        val methodName7 = "testPow_ReturnAccum"
        val methodName8 = "testPow_KEqualsZero"
        val methodName9 = "testPow_KBitwiseAnd1EqualsZero"
        val methodName10 = "testPow_KBitwiseAnd1NotEqualsZero"
        val methodName11 = "testPow_KBitwiseAnd1NotEqualsZero_1"
        val methodName12 = "testPow_KBitwiseAnd1EqualsZero_1"
        val methodName13 = "testPow_KBitwiseAnd1NotEqualsZero_2"
        val methodName14 = "testPow_KBitwiseAnd1EqualsZero_2"

        val displayName1 = "switch(b) case: 1 -> return 1"
        val displayName2 = "k < Integer.SIZE : False -> return 0"
        val displayName3 = "k < Integer.SIZE : False -> return (k < Integer.SIZE) ? (1 << k) : 0"
        val displayName4 = "-> return b * accum" // TODO: weird display name with missed part before ->
        val displayName5 = "k < Integer.SIZE : True -> return (k < Integer.SIZE) ? (1 << k) : 0"
        val displayName6 = "k == 0 : False -> return (k == 0) ? 1 : 0"
        val displayName7 = "-> return accum" // TODO: weird display name with missed part before ->
        val displayName8 = "k == 0 : True -> return (k == 0) ? 1 : 0"
        val displayName9 = "(k & 1) == 0 : True -> return ((k & 1) == 0) ? (1 << k) : -(1 << k)"
        val displayName10 = "(k & 1) == 0 : False -> return ((k & 1) == 0) ? (1 << k) : -(1 << k)"
        val displayName11 = "(k & 1) == 0 : False -> return ((k & 1) == 0) ? 1 : -1"
        val displayName12 = "(k & 1) == 0 : True -> return ((k & 1) == 0) ? 1 : -1"
        val displayName13 = "(k & 1) == 0 : False -> return b * accum"
        val displayName14 = "(k & 1) == 0 : True -> return b * accum"

        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3,
            summary4,
            summary5,
            summary6,
            summary7,
            summary8,
            summary9,
            summary10,
            summary11,
            summary12,
            summary13,
            summmary14
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
            displayName9,
            displayName10,
            displayName11,
            displayName12,
            displayName13,
            displayName14
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
            methodName9,
            methodName10,
            methodName11,
            methodName12,
            methodName13,
            methodName14
        )

        val clusterInfo = listOf(
            Pair(UtClusterInfo("SYMBOLIC EXECUTION: SUCCESSFUL EXECUTIONS for method pow(int, int)", null), 14)
        )

        val method = IntMath::pow
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames, clusterInfo)
    }
}