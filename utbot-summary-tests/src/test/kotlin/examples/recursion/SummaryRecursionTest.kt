package examples.recursion

import examples.CustomJavaDocTagsEnabler
import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.examples.recursion.Recursion
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate

@ExtendWith(CustomJavaDocTagsEnabler::class)
class SummaryRecursionTest : SummaryTestCaseGeneratorTest(
    Recursion::class
) {
    @Test
    fun testFib() {
        val summary1 = "@utbot.classUnderTest {@link Recursion}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.recursion.Recursion#fib(int)}\n" +
                "@utbot.executesCondition {@code (n == 0): False}\n" +
                "@utbot.executesCondition {@code (n == 1): True}\n" +
                "@utbot.returnsFrom {@code return 1;}"
        val summary2 = "@utbot.classUnderTest {@link Recursion}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.recursion.Recursion#fib(int)}\n" +
                "@utbot.executesCondition {@code (n == 0): True}\n" +
                "@utbot.returnsFrom {@code return 0;}\n"
        val summary3 = "@utbot.classUnderTest {@link Recursion}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.recursion.Recursion#fib(int)}\n" +
                "@utbot.executesCondition {@code (n == 1): False}\n" +
                "@utbot.invokes {@link org.utbot.examples.recursion.Recursion#fib(int)}\n" +
                "@utbot.invokes {@link org.utbot.examples.recursion.Recursion#fib(int)}\n" +
                "@utbot.triggersRecursion fib, where the test execute conditions:\n" +
                "    {@code (n == 1): True}\n" +
                "return from: {@code return 1;}" +
                "@utbot.returnsFrom {@code return fib(n - 1) + fib(n - 2);}"
        val summary4 = "@utbot.classUnderTest {@link Recursion}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.recursion.Recursion#fib(int)}\n" +
                "@utbot.executesCondition {@code (n < 0): True}\n" +
                "@utbot.throwsException {@link java.lang.IllegalArgumentException} when: n < 0"

        val methodName1 = "testFib_Return1"
        val methodName2 = "testFib_ReturnZero"
        val methodName3 = "testFib_NNotEquals1"
        val methodName4 = "testFib_ThrowIllegalArgumentException"

        val displayName1 = "n == 0 : False -> return 1"
        val displayName2 = "n == 0 : True -> return 0"
        val displayName3 = "return 1 -> return 0" //it looks weird
        val displayName4 = "n < 0 -> ThrowIllegalArgumentException"

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

        val method = Recursion::fib
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testFactorial() {
        val summary1 = "@utbot.classUnderTest {@link Recursion}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.recursion.Recursion#factorial(int)}\n" +
                //TODO: Lost information about executed condition,
                // see [issue-900](https://github.com/UnitTestBot/UTBotJava/issues/900)
                //"@utbot.executesCondition {@code (n == 0): True}\n"
                "@utbot.returnsFrom {@code return 1;}"
        val summary2 = "@utbot.classUnderTest {@link Recursion}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.recursion.Recursion#factorial(int)}\n" +
                "@utbot.executesCondition {@code (n == 0): False}\n" +
                "@utbot.invokes {@link org.utbot.examples.recursion.Recursion#factorial(int)}\n" +
                "@utbot.triggersRecursion factorial, where the test return from: {@code return 1;}" +
                "@utbot.returnsFrom {@code return n * factorial(n - 1);}"
        val summary3 = "@utbot.classUnderTest {@link Recursion}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.recursion.Recursion#factorial(int)}\n" +
                "@utbot.executesCondition {@code (n < 0): True}\n" +
                "@utbot.throwsException {@link java.lang.IllegalArgumentException} when: n < 0"

        val methodName1 = "testFactorial_Return1"
        val methodName2 = "testFactorial_NNotEqualsZero"
        val methodName3 = "testFactorial_ThrowIllegalArgumentException"

        //TODO: Display names are not complete, see [issue-899](https://github.com/UnitTestBot/UTBotJava/issues/899).
        //they should be equal "n == 0 : True -> return 1" and "n == 0 : False -> return n * factorial(n - 1)" respectively
        val displayName1 = "-> return 1"
        val displayName2 = "-> return 1"
        val displayName3 = "n < 0 -> ThrowIllegalArgumentException"

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

        val method = Recursion::factorial
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}