package examples.controlflow

import examples.SummaryTestCaseGeneratorTest
import org.junit.Ignore
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.utbot.examples.controlflow.Cycles
import org.junit.jupiter.api.Test
@Disabled
class SummaryCycleTest : SummaryTestCaseGeneratorTest(
    Cycles::class,
) {

    val summaryLoopInsideLoop1 = "<pre>\n" +
            "Test iterates the loop {@code for(int i = x - 5; i < x; i++)} once,\n" +
            "    inside this loop, the test executes conditions:\n" +
            "    {@code (i < 0): True}\n" +
            "returns from: {@code return 2;}\n" +
            "</pre>"
    val summaryLoopInsideLoop2 = "<pre>\n" +
            "Test does not iterate {@code for(int i = x - 5; i < x; i++)}, {@code for(int j = i; j < x + i; j++)}, returns from: {@code return -1;}\n" +
            "</pre>"
    val summaryLoopInsideLoop3 = "<pre>\n" +
            "Test iterates the loop {@code for(int i = x - 5; i < x; i++)} once,\n" +
            "    inside this loop, the test executes conditions:\n" +
            "    {@code (i < 0): False}\n" +
            "iterates the loop {@code for(int j = i; j < x + i; j++)} once,\n" +
            "        inside this loop, the test executes conditions:\n" +
            "    {@code (j == 7): True}\n" +
            "returns from: {@code return 1;}\n" +
            "</pre>"
    val summaryLoopInsideLoop4 = "<pre>\n" +
            "Test iterates the loop {@code for(int i = x - 5; i < x; i++)} once,\n" +
            "    inside this loop, the test executes conditions:\n" +
            "    {@code (i < 0): False}\n" +
            "iterates the loop {@code for(int j = i; j < x + i; j++)} twice,\n" +
            "        inside this loop, the test executes conditions:\n" +
            "    {@code (j == 7): False}\n" +
            "    {@code (j == 7): True}\n" +
            "returns from: {@code return 1;}\n" +
            "</pre>"
    val summaryLoopInsideLoop5 = "<pre>\n" +
            "Test iterates the loop {@code for(int i = x - 5; i < x; i++)} 5 times."

    @Test
    fun testLoopInsideLoop() {
        checkOneArgument(
            Cycles::loopInsideLoop,
            summaryKeys = listOf(
                summaryLoopInsideLoop1,
                summaryLoopInsideLoop2,
                summaryLoopInsideLoop3,
                summaryLoopInsideLoop4,
                summaryLoopInsideLoop5
            ),
            displayNames = listOf(
                "i < 0 : True -> return 2",
                "-> return -1",
                "i < 0 : False -> return 1",
                "j == 7 : False -> return 1",
                "-> return -1"
            )
        )
    }

    val summaryStructureLoop1 = "<pre>\n" +
            "Test does not iterate {@code for(int i = 0; i < x; i++)}, returns from: {@code return -1;}\n" +
            "</pre>"
    val summaryStructureLoop2 = "<pre>\n" +
            "Test iterates the loop {@code for(int i = 0; i < x; i++)} once,\n" +
            "    inside this loop, the test executes conditions:\n" +
            "    {@code (i == 2): False}\n"
    val summaryStructureLoop3 = "<pre>\n" +
            "Test iterates the loop {@code for(int i = 0; i < x; i++)} 3 times,\n" +
            "    inside this loop, the test executes conditions:\n" +
            "    {@code (i == 2): True}\n" +
            "returns from: {@code return 1;}\n" +
            "</pre>"

    @Test
    fun testStructureLoop() {
        checkOneArgument(
            Cycles::structureLoop,
            summaryKeys = listOf(
                summaryStructureLoop1,
                summaryStructureLoop2,
                summaryStructureLoop3
            ),
            displayNames = listOf(
                "-> return -1",
                "i == 2 : False -> return -1",
                "i == 2 : True -> return 1"
            )
        )
    }

}