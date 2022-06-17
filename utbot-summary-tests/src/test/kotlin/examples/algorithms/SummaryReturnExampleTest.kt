package examples.algorithms

import examples.SummaryTestCaseGeneratorTest
import org.junit.Ignore
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.utbot.examples.algorithms.ReturnExample
import org.junit.jupiter.api.Test
@Disabled
class SummaryReturnExampleTest : SummaryTestCaseGeneratorTest(
    ReturnExample::class,
) {

    val summaryCompare1 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (a < 0): True}\n" +
            "returns from:\n" +
            "    1st return statement: {@code return a;}\n" +
            "</pre>"
    val summaryCompare2 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (a < 0): False},\n" +
            "    {@code (b < 0): True}\n" +
            "returns from:\n" +
            "    1st return statement: {@code return a;}\n" +
            "</pre>"
    val summaryCompare3 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (a < 0): False},\n" +
            "    {@code (b < 0): False},\n" +
            "    {@code (b == 10): True}\n" +
            "returns from:\n" +
            "    1st return statement: {@code return c;}\n" +
            "</pre>"
    val summaryCompare4 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (a < 0): False},\n" +
            "    {@code (b < 0): False},\n" +
            "    {@code (b == 10): False},\n" +
            "    {@code (a > b): False},\n" +
            "    {@code (a < b): True}\n" +
            "returns from:\n" +
            "    2nd return statement: {@code return a;}\n" +
            "</pre>"
    val summaryCompare5 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (a < 0): False},\n" +
            "    {@code (b < 0): False},\n" +
            "    {@code (b == 10): False},\n" +
            "    {@code (a > b): True}\n" +
            "returns from: {@code return b;}\n" +
            "</pre>"
    val summaryCompare6 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (a < 0): False},\n" +
            "    {@code (b < 0): False},\n" +
            "    {@code (b == 10): False},\n" +
            "    {@code (a > b): False},\n" +
            "    {@code (a < b): False}\n" +
            "returns from:\n" +
            "    2nd return statement: {@code return c;}\n" +
            "</pre>"

    @Test
    fun testCompare() {
        checkTwoArguments(
            ReturnExample::compare,
            summaryKeys = listOf(
                summaryCompare1,
                summaryCompare2,
                summaryCompare3,
                summaryCompare4,
                summaryCompare5,
                summaryCompare6
            ),
            displayNames = listOf(
                "a < 0 : False -> return a",
                "b < 0 : True -> return a",
                "b == 10 : True -> return c",
                "a < b : True -> return a",
                "a > b : True -> return b",
                "a < b : False -> return c"
            )
        )
    }

    val summaryCompareChars1 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (n < 1): True}\n" +
            "returns from: {@code return ' ';}\n" +
            "</pre>"
    val summaryCompareChars2 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (n < 1): False}\n" +
            "iterates the loop {@code for(int i = 0; i < n; i++)} once,\n" +
            "    inside this loop, the test executes conditions:\n" +
            "    {@code (Character.toChars(i)[0] == a): True}\n" +
            "returns from: {@code return b;}\n" +
            "</pre>"
    val summaryCompareChars3 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (n < 1): False}\n" +
            "iterates the loop {@code for(int i = 0; i < n; i++)} once,\n" +
            "    inside this loop, the test executes conditions:\n" +
            "    {@code (Character.toChars(i)[0] == a): False},\n" +
            "    {@code (Character.toChars(i)[0] == b): True}\n" +
            "returns from:\n" +
            "    1st return statement: {@code return a;}\n" +
            "</pre>"
    val summaryCompareChars4 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (n < 1): False}\n" +
            "iterates the loop {@code for(int i = 0; i < n; i++)} once,\n" +
            "    inside this loop, the test executes conditions:\n" +
            "    {@code (Character.toChars(i)[0] == a): False},\n" +
            "    {@code (Character.toChars(i)[0] == b): False}"

    @Test
    fun testCompareChar() {
        checkThreeArguments(
            ReturnExample::compareChars,
            summaryKeys = listOf(
                summaryCompareChars1,
                summaryCompareChars2,
                summaryCompareChars3,
                summaryCompareChars4
            ),
            displayNames = listOf(
                "n < 1 : True -> return ' '",
                "Character.toChars(i)[0] == a : True -> return b",
                "Character.toChars(i)[0] == b : True -> return a",
                "Character.toChars(i)[0] == b : False -> return a"
            )
        )
    }

    val summaryVoidCompareChars1 = "<pre>\n" +
            "Test calls ReturnExample::compareChars,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n < 1): True}\n" +
            "    returns from: {@code return ' ';}\n" +
            "</pre>"
    val summaryVoidCompareChars2 = "<pre>\n" +
            "Test calls ReturnExample::compareChars,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n < 1): False}\n" +
            "    iterates the loop {@code for(int i = 0; i < n; i++)} once,\n" +
            "        inside this loop, the test executes conditions:\n" +
            "        {@code (Character.toChars(i)[0] == a): True}\n" +
            "    returns from: {@code return b;}\n" +
            "</pre>"
    val summaryVoidCompareChars3 = "<pre>\n" +
            "Test calls ReturnExample::compareChars,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n < 1): False}\n" +
            "    iterates the loop {@code for(int i = 0; i < n; i++)} once,\n" +
            "        inside this loop, the test executes conditions:\n" +
            "        {@code (Character.toChars(i)[0] == a): False},\n" +
            "        {@code (Character.toChars(i)[0] == b): True}\n" +
            "    returns from: {@code return a;}\n" +
            "</pre>"
    val summaryVoidCompareChars4 = "Test calls ReturnExample::compareChars,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n < 1): False}\n" +
            "    iterates the loop {@code for(int i = 0; i < n; i++)} once,\n" +
            "        inside this loop, the test executes conditions:\n" +
            "        {@code (Character.toChars(i)[0] == a): False},\n" +
            "        {@code (Character.toChars(i)[0] == b): False}"


    @Test
    fun testInnerVoidCompareChars() {
        checkThreeArguments(
            ReturnExample::innerVoidCompareChars,
            summaryKeys = listOf(
                summaryVoidCompareChars1,
                summaryVoidCompareChars2,
                summaryVoidCompareChars3,
                summaryVoidCompareChars4
            ),
            displayNames = listOf(
                "n < 1 : True -> return ' '",
                "Character.toChars(i)[0] == a : True -> return b",
                "Character.toChars(i)[0] == b : True -> return a",
                "Character.toChars(i)[0] == b : False -> return a"
            )
        )
    }

    val summaryInnerReturnCompareChars1 = "<pre>\n" +
            "Test calls ReturnExample::compareChars,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n < 1): True}\n" +
            "    returns from: {@code return ' ';}"
    val summaryInnerReturnCompareChars2 = "Test calls ReturnExample::compareChars,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n < 1): False}\n" +
            "    iterates the loop {@code for(int i = 0; i < n; i++)} once,\n" +
            "        inside this loop, the test executes conditions:\n" +
            "        {@code (Character.toChars(i)[0] == a): True}\n" +
            "    returns from: {@code return b;}"
    val summaryInnerReturnCompareChars3 = "Test calls ReturnExample::compareChars,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n < 1): False}\n" +
            "    iterates the loop {@code for(int i = 0; i < n; i++)} once,\n" +
            "        inside this loop, the test executes conditions:\n" +
            "        {@code (Character.toChars(i)[0] == a): False},\n" +
            "        {@code (Character.toChars(i)[0] == b): True}\n" +
            "    returns from: {@code return a;}"
    val summaryInnerReturnCompareChars4 = "Test calls ReturnExample::compareChars,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n < 1): False}\n" +
            "    iterates the loop {@code for(int i = 0; i < n; i++)} once,\n" +
            "        inside this loop, the test executes conditions:\n" +
            "        {@code (Character.toChars(i)[0] == a): False},\n" +
            "        {@code (Character.toChars(i)[0] == b): False}"

    @Test
    fun testInnerReturnCompareChars() {
        checkThreeArguments(
            ReturnExample::innerReturnCompareChars,
            summaryKeys = listOf(
                summaryInnerReturnCompareChars1,
                summaryInnerReturnCompareChars2,
                summaryInnerReturnCompareChars3,
                summaryInnerReturnCompareChars4
            )
        )
    }

    val summaryInnerVoidCompare1 = "<pre>\n" +
            "Test calls ReturnExample::compare,\n" +
            "    there it executes conditions:\n" +
            "        {@code (a < 0): False},\n" +
            "        {@code (b < 0): True}\n" +
            "    returns from: {@code return a;}\n" +
            "</pre>"
    val summaryInnerVoidCompare2 = "<pre>\n" +
            "Test calls ReturnExample::compare,\n" +
            "    there it executes conditions:\n" +
            "        {@code (a < 0): False},\n" +
            "        {@code (b < 0): False},\n" +
            "        {@code (b == 10): True}\n" +
            "    returns from: {@code return c;}\n" +
            "</pre>"
    val summaryInnerVoidCompare3 = "<pre>\n" +
            "Test calls ReturnExample::compare,\n" +
            "    there it executes conditions:\n" +
            "        {@code (a < 0): False},\n" +
            "        {@code (b < 0): False},\n" +
            "        {@code (b == 10): False},\n" +
            "        {@code (a > b): False},\n" +
            "        {@code (a < b): False}\n" +
            "    returns from: {@code return c;}\n" +
            "</pre>"
    val summaryInnerVoidCompare4 = "<pre>\n" +
            "Test calls ReturnExample::compare,\n" +
            "    there it executes conditions:\n" +
            "        {@code (a < 0): False},\n" +
            "        {@code (b < 0): False},\n" +
            "        {@code (b == 10): False},\n" +
            "        {@code (a > b): False},\n" +
            "        {@code (a < b): True}\n" +
            "    returns from: {@code return a;}\n" +
            "</pre>"
    val summaryInnerVoidCompare5 = "<pre>\n" +
            "Test calls ReturnExample::compare,\n" +
            "    there it executes conditions:\n" +
            "        {@code (a < 0): True}\n" +
            "    returns from: {@code return a;}\n" +
            "</pre>"
    val summaryInnerVoidCompare6 = "<pre>\n" +
            "Test calls ReturnExample::compare,\n" +
            "    there it executes conditions:\n" +
            "        {@code (a < 0): False},\n" +
            "        {@code (b < 0): False},\n" +
            "        {@code (b == 10): False},\n" +
            "        {@code (a > b): True}\n" +
            "    returns from: {@code return b;}\n" +
            "</pre>"

    @Test
    fun testInnerVoidCompare() {
        checkTwoArguments(
            ReturnExample::innerVoidCallCompare,
            summaryKeys = listOf(
                summaryInnerVoidCompare1,
                summaryInnerVoidCompare2,
                summaryInnerVoidCompare3,
                summaryInnerVoidCompare4,
                summaryInnerVoidCompare5,
                summaryInnerVoidCompare6
            ),
            displayNames = listOf(
                "b < 0 : True -> return a",
                "b == 10 : True -> return c",
                "a < b : False -> return c",
                "a < b : True -> return a",
                "a < 0 : False -> return a",
                "a > b : True -> return b"
            )
        )
    }

    val summaryInnerReturnCompare1 = "Test calls ReturnExample::compare,\n" +
            "    there it executes conditions:\n" +
            "        {@code (a < 0): False},\n" +
            "        {@code (b < 0): True}\n" +
            "    returns from: {@code return a;}"
    val summaryInnerReturnCompare2 = "Test calls ReturnExample::compare,\n" +
            "    there it executes conditions:\n" +
            "        {@code (a < 0): False},\n" +
            "        {@code (b < 0): False},\n" +
            "        {@code (b == 10): True}\n" +
            "    returns from: {@code return c;}"
    val summaryInnerReturnCompare3 = "Test calls ReturnExample::compare,\n" +
            "    there it executes conditions:\n" +
            "        {@code (a < 0): False},\n" +
            "        {@code (b < 0): False},\n" +
            "        {@code (b == 10): False},\n" +
            "        {@code (a > b): False},\n" +
            "        {@code (a < b): False}\n" +
            "    returns from: {@code return c;}"
    val summaryInnerReturnCompare4 = "Test calls ReturnExample::compare,\n" +
            "    there it executes conditions:\n" +
            "        {@code (a < 0): False},\n" +
            "        {@code (b < 0): False},\n" +
            "        {@code (b == 10): False},\n" +
            "        {@code (a > b): False},\n" +
            "        {@code (a < b): True}\n" +
            "    returns from: {@code return a;}"
    val summaryInnerReturnCompare5 = "Test calls ReturnExample::compare,\n" +
            "    there it executes conditions:\n" +
            "        {@code (a < 0): True}\n" +
            "    returns from: {@code return a;}"
    val summaryInnerReturnCompare6 = "Test calls ReturnExample::compare,\n" +
            "    there it executes conditions:\n" +
            "        {@code (a < 0): False},\n" +
            "        {@code (b < 0): False},\n" +
            "        {@code (b == 10): False},\n" +
            "        {@code (a > b): True}\n" +
            "    returns from: {@code return b;}"

    @Test
    fun testInnerReturnCompare() {
        checkTwoArguments(
            ReturnExample::innerReturnCallCompare,
            summaryKeys = listOf(
                summaryInnerReturnCompare1,
                summaryInnerReturnCompare2,
                summaryInnerReturnCompare3,
                summaryInnerReturnCompare4,
                summaryInnerReturnCompare5,
                summaryInnerReturnCompare6
            ),
            displayNames = listOf(
                "b < 0 : True -> return a",
                "b == 10 : True -> return c",
                "a < b : False -> return c",
                "a < b : True -> return a",
                "a < 0 : False -> return a",
                "a > b : True -> return b"
            )
        )
    }
}