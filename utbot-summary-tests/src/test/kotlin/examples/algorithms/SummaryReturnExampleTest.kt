package examples.algorithms

import examples.SummaryTestCaseGeneratorTest
import org.utbot.examples.algorithms.ReturnExample
import org.junit.jupiter.api.Test
import org.utbot.examples.DoNotCalculate
import org.utbot.framework.plugin.api.MockStrategyApi

class SummaryReturnExampleTest : SummaryTestCaseGeneratorTest(
    ReturnExample::class,
) {
    @Test
    fun testCompare() {
        val summary1 = "Test executes conditions:\n" +
                "    (a < 0): True\n" +
                "returns from:\n" +
                "    1st return statement: return a;\n"
        val summary2 = "Test executes conditions:\n" +
                "    (a < 0): False,\n" +
                "    (b < 0): True\n" +
                "returns from:\n" +
                "    1st return statement: return a;\n"
        val summary3 = "Test executes conditions:\n" +
                "    (a < 0): False,\n" +
                "    (b < 0): False,\n" +
                "    (b == 10): True\n" +
                "returns from:\n" +
                "    1st return statement: return c;\n"
        val summary4 = "Test executes conditions:\n" +
                "    (a < 0): False,\n" +
                "    (b < 0): False,\n" +
                "    (b == 10): False,\n" +
                "    (a > b): False,\n" +
                "    (a < b): True\n" +
                "returns from:\n" +
                "    2nd return statement: return a;\n"
        val summary5 = "Test executes conditions:\n" +
                "    (a < 0): False,\n" +
                "    (b < 0): False,\n" +
                "    (b == 10): False,\n" +
                "    (a > b): False,\n" +
                "    (a < b): False\n" +
                "returns from:\n" +
                "    2nd return statement: return c;\n"
        val summary6 = "Test executes conditions:\n" +
                "    (a < 0): False,\n" +
                "    (b < 0): False,\n" +
                "    (b == 10): False,\n" +
                "    (a > b): True\n" +
                "returns from: return b;\n"

        val methodName1 = "testCompare_ALessThanZero"
        val methodName2 = "testCompare_BLessThanZero"
        val methodName3 = "testCompare_BEquals10"
        val methodName4 = "testCompare_ALessThanB"
        val methodName5 = "testCompare_AGreaterOrEqualB"
        val methodName6 = "testCompare_AGreaterThanB"

        val displayName1 = "a < 0 : False -> return a"
        val displayName2 = "b < 0 : True -> return a"
        val displayName3 = "b == 10 : True -> return c"
        val displayName4 = "a < b : True -> return a"
        val displayName5 = "a < b : False -> return c"
        val displayName6 = "a > b : True -> return b"

        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3,
            summary4,
            summary5,
            summary6
        )

        val displayNames = listOf(
            displayName1,
            displayName2,
            displayName3,
            displayName4,
            displayName5,
            displayName6
        )

        val methodNames = listOf(
            methodName1,
            methodName2,
            methodName3,
            methodName4,
            methodName5,
            methodName6
        )

        val method = ReturnExample::compare
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testCompareChar() {
        val summary1 = "Test executes conditions:\n" +
                "    (n < 1): True\n" +
                "returns from: return ' ';\n"
        val summary2 = "Test executes conditions:\n" +
                "    (n < 1): False\n" +
                "iterates the loop for(int i = 0; i < n; i++) once,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    (Character.toChars(i)[0] == a): True\n" +
                "returns from: return b;"
        val summary3 = "Test executes conditions:\n" +
                "    (n < 1): False\n" +
                "iterates the loop for(int i = 0; i < n; i++) once,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    (Character.toChars(i)[0] == a): False,\n" +
                "    (Character.toChars(i)[0] == b): True\n" +
                "returns from:\n" +
                "    1st return statement: return a;"
        val summary4 = "Test executes conditions:\n" +
                "    (n < 1): False\n" +
                "iterates the loop for(int i = 0; i < n; i++) once,\n" +
                "    inside this loop, the test executes conditions:\n" +
                "    (Character.toChars(i)[0] == a): False,\n" +
                "    (Character.toChars(i)[0] == b): False\n" +
                "Test then returns from:\n" +
                "    2nd return statement: return a;\n"

        val methodName1 = "testCompareChars_NLessThan1"
        val methodName2 = "testCompareChars_0OfCharacterToCharsIEqualsA" // TODO: a weird unclear naming
        val methodName3 = "testCompareChars_0OfCharacterToCharsIEqualsB"
        val methodName4 = "testCompareChars_0OfCharacterToCharsINotEqualsB"

        val displayName1 = "n < 1 : True -> return ' '"
        val displayName2 = "Character.toChars(i)[0] == a : True -> return b"
        val displayName3 = "Character.toChars(i)[0] == b : True -> return a"
        val displayName4 = "Character.toChars(i)[0] == b : False -> return a"

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

        val method = ReturnExample::compareChars
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testInnerVoidCompareChars() {
        val summary1 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compareChars(char,char,int)},\n" +
                "    there it executes conditions:\n" +
                "        (n < 1): True\n" +
                "    returns from: return ' ';\n" +
                "    " // TODO: generates empty String or \n a the end
        val summary2 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compareChars(char,char,int)},\n" +
                "    there it executes conditions:\n" +
                "        (n < 1): False\n" +
                "    iterates the loop for(int i = 0; i < n; i++) once,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "        (Character.toChars(i)[0] == a): True\n" +
                "    returns from: return b;"
        val summary3 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compareChars(char,char,int)},\n" +
                "    there it executes conditions:\n" +
                "        (n < 1): False\n" +
                "    iterates the loop for(int i = 0; i < n; i++) once,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "        (Character.toChars(i)[0] == a): False,\n" +
                "        (Character.toChars(i)[0] == b): False\n" +
                "    Test then returns from: return a;\n" +
                "    " // TODO: generates empty String or \n a the end
        val summary4 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compareChars(char,char,int)},\n" +
                "    there it executes conditions:\n" +
                "        (n < 1): False\n" +
                "    iterates the loop for(int i = 0; i < n; i++) once,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "        (Character.toChars(i)[0] == a): False,\n" +
                "        (Character.toChars(i)[0] == b): True\n" +
                "    returns from: return a;"

        val methodName1 = "testInnerVoidCompareChars_NLessThan1"
        val methodName2 = "testInnerVoidCompareChars_0OfCharacterToCharsIEqualsA" // TODO: a weird unclear naming
        val methodName3 = "testInnerVoidCompareChars_0OfCharacterToCharsINotEqualsB"
        val methodName4 = "testInnerVoidCompareChars_0OfCharacterToCharsIEqualsB"

        val displayName1 = "n < 1 : True -> return ' '"
        val displayName2 = "Character.toChars(i)[0] == a : True -> return b"
        val displayName3 = "Character.toChars(i)[0] == b : False -> return a"
        val displayName4 = "Character.toChars(i)[0] == b : True -> return a"

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

        val method = ReturnExample::innerVoidCompareChars
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testInnerReturnCompareChars() {
        val summary1 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compareChars(char,char,int)},\n" +
                "    there it executes conditions:\n" +
                "        (n < 1): True\n" +
                "    returns from: return ' ';\n" +
                "    \n" +
                "Test later returns from: return compareChars(a, b, n);\n"
        val summary2 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compareChars(char,char,int)},\n" +
                "    there it executes conditions:\n" +
                "        (n < 1): False\n" +
                "    iterates the loop for(int i = 0; i < n; i++) once,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "        (Character.toChars(i)[0] == a): True\n" +
                "    returns from: return b;\n" +
                "Test later returns from: return compareChars(a, b, n);\n"
        val summary3 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compareChars(char,char,int)},\n" +
                "    there it executes conditions:\n" +
                "        (n < 1): False\n" +
                "    iterates the loop for(int i = 0; i < n; i++) once,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "        (Character.toChars(i)[0] == a): False,\n" +
                "        (Character.toChars(i)[0] == b): False\n" +
                "    Test then returns from: return a;\n" +
                "    \n" + //
                "Test afterwards returns from: return compareChars(a, b, n);\n"
        val summary4 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compareChars(char,char,int)},\n" +
                "    there it executes conditions:\n" +
                "        (n < 1): False\n" +
                "    iterates the loop for(int i = 0; i < n; i++) once,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "        (Character.toChars(i)[0] == a): False,\n" +
                "        (Character.toChars(i)[0] == b): True\n" +
                "    returns from: return a;\n" +
                "Test afterwards returns from: return compareChars(a, b, n);\n"

        val methodName1 = "testInnerReturnCompareChars_NLessThan1"
        val methodName2 = "testInnerReturnCompareChars_0OfCharacterToCharsIEqualsA" // TODO: a weird unclear naming
        val methodName3 = "testInnerReturnCompareChars_0OfCharacterToCharsINotEqualsB"
        val methodName4 = "testInnerReturnCompareChars_0OfCharacterToCharsIEqualsB"

        val displayName1 = "n < 1 : True -> return ' '"
        val displayName2 = "Character.toChars(i)[0] == a : True -> return b"
        val displayName3 = "Character.toChars(i)[0] == b : False -> return a"
        val displayName4 = "Character.toChars(i)[0] == b : True -> return a"

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

        val method = ReturnExample::innerReturnCompareChars
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testInnerVoidCompare() {
        val summary1 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compare(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (a < 0): False,\n" +
                "        (b < 0): True\n" +
                "    returns from: return a;\n" +
                "    " // TODO: remove blank line
        val summary2 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compare(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (a < 0): False,\n" +
                "        (b < 0): False,\n" +
                "        (b == 10): False,\n" +
                "        (a > b): True\n" +
                "    returns from: return b;\n" +
                "    " // TODO: remove blank line
        val summary3 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compare(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (a < 0): False,\n" +
                "        (b < 0): False,\n" +
                "        (b == 10): True\n" +
                "    returns from: return c;\n" +
                "    " // TODO: remove blank line
        val summary4 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compare(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (a < 0): True\n" +
                "    returns from: return a;\n" +
                "    " // TODO: remove blank line
        val summary5 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compare(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (a < 0): False,\n" +
                "        (b < 0): False,\n" +
                "        (b == 10): False,\n" +
                "        (a > b): False,\n" +
                "        (a < b): True\n" +
                "    returns from: return a;\n" +
                "    " // TODO: remove blank line
        val summary6 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compare(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (a < 0): False,\n" +
                "        (b < 0): False,\n" +
                "        (b == 10): False,\n" +
                "        (a > b): False,\n" +
                "        (a < b): False\n" +
                "    returns from: return c;\n" +
                "    " // TODO: remove blank line

        val methodName1 = "testInnerVoidCallCompare_BLessThanZero"
        val methodName2 = "testInnerVoidCallCompare_AGreaterThanB"
        val methodName3 = "testInnerVoidCallCompare_BEquals10"
        val methodName4 = "testInnerVoidCallCompare_ALessThanZero"
        val methodName5 = "testInnerVoidCallCompare_ALessThanB"
        val methodName6 = "testInnerVoidCallCompare_AGreaterOrEqualB"

        val displayName1 = "b < 0 : True -> return a"
        val displayName2 = "a > b : True -> return b"
        val displayName3 = "b == 10 : True -> return c"
        val displayName4 = "a < 0 : False -> return a"
        val displayName5 = "a < b : True -> return a"
        val displayName6 = "a < b : False -> return c"

        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3,
            summary4,
            summary5,
            summary6
        )

        val displayNames = listOf(
            displayName1,
            displayName2,
            displayName3,
            displayName4,
            displayName5,
            displayName6
        )

        val methodNames = listOf(
            methodName1,
            methodName2,
            methodName3,
            methodName4,
            methodName5,
            methodName6
        )

        val method = ReturnExample::innerVoidCallCompare
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testInnerReturnCompare() {
        val summary1 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compare(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (a < 0): False,\n" +
                "        (b < 0): True\n" +
                "    returns from: return a;\n" +
                "    \n" +
                "Test then returns from: return compare(a, b);\n"
        val summary2 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compare(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (a < 0): False,\n" +
                "        (b < 0): False,\n" +
                "        (b == 10): False,\n" +
                "        (a > b): True\n" +
                "    returns from: return b;\n" +
                "    \n" +
                "Test afterwards returns from: return compare(a, b);\n"
        val summary3 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compare(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (a < 0): False,\n" +
                "        (b < 0): False,\n" +
                "        (b == 10): True\n" +
                "    returns from: return c;\n" +
                "    \n" +
                "Test then returns from: return compare(a, b);\n"
        val summary4 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compare(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (a < 0): True\n" +
                "    returns from: return a;\n" +
                "    \n" +
                "Test next returns from: return compare(a, b);\n"
        val summary5 = "Test calls{@link org.utbot.examples.algorithms.ReturnExample#compare(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (a < 0): False,\n" +
                "        (b < 0): False,\n" +
                "        (b == 10): False,\n" +
                "        (a > b): False,\n" +
                "        (a < b): True\n" +
                "    returns from: return a;\n" +
                "    \n" +
                "Test afterwards returns from: return compare(a, b);\n"
        val summary6 = "Test calls {@link org.utbot.examples.algorithms.ReturnExample#compare(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (a < 0): False,\n" +
                "        (b < 0): False,\n" +
                "        (b == 10): False,\n" +
                "        (a > b): False,\n" +
                "        (a < b): False\n" +
                "    returns from: return c;\n" +
                "    \n" +
                "Test next returns from: return compare(a, b);\n"

        val methodName1 = "testInnerReturnCallCompare_BLessThanZero"
        val methodName2 = "testInnerReturnCallCompare_AGreaterThanB"
        val methodName3 = "testInnerReturnCallCompare_BEquals10"
        val methodName4 = "testInnerReturnCallCompare_ALessThanZero"
        val methodName5 = "testInnerReturnCallCompare_ALessThanB"
        val methodName6 = "testInnerReturnCallCompare_AGreaterOrEqualB"

        val displayName1 =
            "b < 0 : True -> return a" // TODO: the same display names for many tests with different test names
        val displayName2 = "a > b : True -> return b"
        val displayName3 = "b == 10 : True -> return c"
        val displayName4 = "a < 0 : False -> return a"
        val displayName5 = "a < b : True -> return a"
        val displayName6 = "a < b : False -> return c"

        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3,
            summary4,
            summary5,
            summary6
        )

        val displayNames = listOf(
            displayName1,
            displayName2,
            displayName3,
            displayName4,
            displayName5,
            displayName6
        )

        val methodNames = listOf(
            methodName1,
            methodName2,
            methodName3,
            methodName4,
            methodName5,
            methodName6
        )

        val method = ReturnExample::innerReturnCallCompare
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}