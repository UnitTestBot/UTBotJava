package examples.controlflow

import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.controlflow.Cycles
import org.utbot.framework.plugin.api.MockStrategyApi

class SummaryCycleTest : SummaryTestCaseGeneratorTest(
    Cycles::class,
) {
    @Test
    fun testLoopInsideLoop() {
        val summary1 = "Test iterates the loop for(int i = x - 5; i < x; i++) once,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    (i < 0): True\n" +
                "returns from: return 2;"
        val summary2 =
            "Test does not iterate for(int i = x - 5; i < x; i++), for(int j = i; j < x + i; j++), returns from: return -1;\n" // TODO: should it be formatted from the new string?
        val summary3 = "Test iterates the loop for(int i = x - 5; i < x; i++) once,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    (i < 0): False\n" +
                "iterates the loop for(int j = i; j < x + i; j++) once,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "    (j == 7): True\n" +
                "returns from: return 1;"
        val summary4 = "Test iterates the loop for(int i = x - 5; i < x; i++) once,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    (i < 0): False\n" +
                "iterates the loop for(int j = i; j < x + i; j++) twice,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "    (j == 7): False\n" +
                "    (j == 7): True\n" +
                "returns from: return 1;"
        val summary5 = "Test iterates the loop for(int i = x - 5; i < x; i++) 5 times. \n" +
                "Test afterwards does not iterate for(int j = i; j < x + i; j++), returns from: return -1;\n" // TODO: should it be formatted with separation of code?

        val methodName1 = "testLoopInsideLoop_ILessThanZero"
        val methodName2 = "testLoopInsideLoop_ReturnNegative1"
        val methodName3 = "testLoopInsideLoop_JEquals7"
        val methodName4 = "testLoopInsideLoop_JNotEquals7"
        val methodName5 = "testLoopInsideLoop_ReturnNegative1_1"


        val displayName1 = "i < 0 : True -> return 2"
        val displayName2 = "-> return -1" // TODO: add something before ->
        val displayName3 = "i < 0 : False -> return 1"
        val displayName4 = "j == 7 : False -> return 1"
        val displayName5 = "-> return -1"


        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3,
            summary4,
            summary5
        )

        val displayNames = listOf(
            displayName1,
            displayName2,
            displayName3,
            displayName4,
            displayName5
        )

        val methodNames = listOf(
            methodName1,
            methodName2,
            methodName3,
            methodName4,
            methodName5
        )

        val method = Cycles::loopInsideLoop
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        check(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testStructureLoop() {
        val summary1 = "Test does not iterate for(int i = 0; i < x; i++), returns from: return -1;\n"
        val summary2 = "Test iterates the loop for(int i = 0; i < x; i++) once,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    (i == 2): False\n" +
                "Test further returns from: return -1;\n"
        val summary3 = "Test iterates the loop for(int i = 0; i < x; i++) 3 times,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    (i == 2): True\n" +
                "returns from: return 1;"

        val methodName1 = "testStructureLoop_ReturnNegative1"
        val methodName2 = "testStructureLoop_INotEquals2"
        val methodName3 = "testStructureLoop_IEquals2"


        val displayName1 = "-> return -1"
        val displayName2 = "i == 2 : False -> return -1"
        val displayName3 = "i == 2 : True -> return 1"


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

        val method = Cycles::structureLoop
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        check(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

}