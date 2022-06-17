package examples.inner

import examples.SummaryTestCaseGeneratorTest
import org.junit.Ignore
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.utbot.examples.inner.InnerCalls
import org.junit.jupiter.api.Test

@Disabled
class SummaryInnerCallsTest : SummaryTestCaseGeneratorTest(
    InnerCalls::class,
) {

    val keyCallLoopInsideLoop1 = "Test calls Cycles::loopInsideLoop,\n" +
            "    there it iterates the loop {@code for(int i = x - 5; i < x; i++)} once,\n" +
            "        inside this loop, the test executes conditions:\n" +
            "        {@code (i < 0): True}\n" +
            "    returns from: {@code return 2;}"
    val keyCallLoopInsideLoop2 = "Test calls Cycles::loopInsideLoop,\n" +
            "    there it iterates the loop {@code for(int i = x - 5; i < x; i++)} once,\n" +
            "        inside this loop, the test executes conditions:\n" +
            "        {@code (i < 0): False}\n" +
            "    iterates the loop {@code for(int j = i; j < x + i; j++)} once,\n" +
            "            inside this loop, the test executes conditions:\n" +
            "        {@code (j == 7): True}\n" +
            "    returns from: {@code return 1;}"
    val keyCallLoopInsideLoop3 = "Test calls Cycles::loopInsideLoop,\n" +
            "    there it does not iterate {@code for(int i = x - 5; i < x; i++)}, {@code for(int j = i; j < x + i; j++)}, returns from: {@code return -1;}"
    val keyCallLoopInsideLoop4 = "Test calls Cycles::loopInsideLoop,\n" +
            "    there it iterates the loop {@code for(int i = x - 5; i < x; i++)} once,\n" +
            "        inside this loop, the test executes conditions:\n" +
            "        {@code (i < 0): False}\n" +
            "    iterates the loop {@code for(int j = i; j < x + i; j++)} twice,\n" +
            "            inside this loop, the test executes conditions:\n" +
            "        {@code (j == 7): False}\n" +
            "        {@code (j == 7): True}\n" +
            "    returns from: {@code return 1;}"
    val keyCallLoopInsideLoop5 = "Test calls Cycles::loopInsideLoop,\n" +
            "    there it iterates the loop {@code for(int i = x - 5; i < x; i++)} 5 times. "

    @Test
    fun testCallLoopInsideLoop() {
        checkOneArgument(
            InnerCalls::callLoopInsideLoop,
            summaryKeys = listOf(
                keyCallLoopInsideLoop1,
                keyCallLoopInsideLoop2,
                keyCallLoopInsideLoop3,
                keyCallLoopInsideLoop4,
                keyCallLoopInsideLoop5
            ),
            displayNames = listOf(
                "i < 0 : True -> return 2",
                "i < 0 : False -> return 1",
                "loopInsideLoop -> return -1",
                "j == 7 : False -> return 1",
                "loopInsideLoop -> return -1"
            )
        )
    }

    val keyInnerCallLeftSearch1 =
        "Test throws IllegalArgumentException in: return binarySearch.leftBinSearch(array, key);"
    val keyInnerCallLeftSearch2 = "Test calls BinarySearch::leftBinSearch,\n" +
            "    there it invokes:\n" +
            "        BinarySearch::isUnsorted once\n" +
            "    triggers recursion of leftBinSearch once.\n" +
            "Test throws NullPointerException in: return binarySearch.leftBinSearch(array, key);"

    val keyInnerCallLeftSearch3 = "Test calls BinarySearch::leftBinSearch,\n" +
            "    there it does not iterate {@code while(left < right - 1)}, executes conditions:\n" +
            "        {@code (found): False}"
    val keyInnerCallLeftSearch4 = "Test calls BinarySearch::leftBinSearch,\n" +
            "    there it executes conditions:\n" +
            "        {@code (isUnsorted(array)): True}\n" +
            "Test throws NullPointerException in: return binarySearch.leftBinSearch(array, key);"

    val keyInnerCallLeftSearch5 = "Test calls BinarySearch::leftBinSearch,\n" +
            "    there it iterates the loop {@code while(left < right - 1)} once,\n" +
            "        inside this loop, the test executes conditions:\n" +
            "        {@code (array[middle] == key): False},\n" +
            "        {@code (array[middle] < key): True}"
    val keyInnerCallLeftSearch6 = "Test calls BinarySearch::leftBinSearch,\n" +
            "    there it iterates the loop {@code while(left < right - 1)} once,\n" +
            "        inside this loop, the test executes conditions:\n" +
            "        {@code (array[middle] == key): False},\n" +
            "        {@code (array[middle] < key): False}"
    val keyInnerCallLeftSearch7 = "Test calls BinarySearch::leftBinSearch,\n" +
            "    there it iterates the loop {@code while(left < right - 1)} once,\n" +
            "        inside this loop, the test executes conditions:\n" +
            "        {@code (array[middle] == key): True},\n" +
            "        {@code (array[middle] < key): False}"


    @Test
    fun testCallLeftBinSearch() {
        checkTwoArguments(
            InnerCalls::callLeftBinSearch,
            summaryKeys = listOf(
                keyInnerCallLeftSearch1,
                keyInnerCallLeftSearch2,
                keyInnerCallLeftSearch3,
                keyInnerCallLeftSearch4,
                keyInnerCallLeftSearch5,
                keyInnerCallLeftSearch6,
                keyInnerCallLeftSearch7
            )
        )
    }

    val keySummaryThreeDimensionalArray1 = "Test calls ArrayOfArrays::createNewThreeDimensionalArray,\n" +
            "    there it executes conditions:\n" +
            "        {@code (length != 2): True}\n" +
            "    returns from: {@code return new int[0][][];}"
    val keySummaryThreeDimensionalArray2 = "Test calls ArrayOfArrays::createNewThreeDimensionalArray,\n" +
            "    there it executes conditions:\n" +
            "        {@code (length != 2): False}"

    // TODO: SAT-1211
    @Test
    fun testCallCreateNewThreeDimensionalArray() {
        checkTwoArguments(
            InnerCalls::callCreateNewThreeDimensionalArray,
            summaryKeys = listOf(
                keySummaryThreeDimensionalArray1,
                keySummaryThreeDimensionalArray2
            ),
            displayNames = listOf(
                "length != 2 : True -> return new int[0][][]",
                "length != 2 : False -> return matrix"
            )
        )
    }

    val summaryCallInitExample1 = "Test calls ExceptionExamples::initAnArray,\n" +
            "    there it catches exception:\n" +
            "        {@code NegativeArraySizeException e}\n" +
            "    returns from: {@code return -2;}"
    val summaryCallInitExample2 = "Test calls ExceptionExamples::initAnArray,\n" +
            "    there it catches exception:\n" +
            "        {@code IndexOutOfBoundsException e}\n" +
            "    returns from: {@code return -3;}"
    val summaryCallInitExample3 = "Test calls ExceptionExamples::initAnArray,\n" +
            "    there it catches exception:\n" +
            "        {@code IndexOutOfBoundsException e}\n" +
            "    returns from: {@code return -3;}"
    val summaryCallInitExample4 = "Test calls ExceptionExamples::initAnArray,\n" +
            "    there it returns from: {@code return a[n - 1] + a[n - 2];}"

    @Test
    fun testCallInitExamples() {
        checkOneArgument(
            InnerCalls::callInitExamples,
            summaryKeys = listOf(
                summaryCallInitExample1,
                summaryCallInitExample2,
                summaryCallInitExample3,
                summaryCallInitExample4
            )
        )
    }

    val summaryCallFactorial1 = "Test calls Recursion::factorial,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n == 0): True}\n" +
            "    returns from: {@code return 1;}"
    val summaryCallFactorial2 = "Test calls Recursion::factorial,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n < 0): True}\n" +
            "    triggers recursion of factorial once."
    val summaryCallFactorial3 = "Test calls Recursion::factorial,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n == 0): False}\n" +
            "    triggers recursion of factorial once, returns from: {@code return n * factorial(n - 1);}"

    @Test
    fun testCallFactorial() {
        checkOneArgument(
            InnerCalls::callFactorial,
            summaryKeys = listOf(
                summaryCallFactorial1,
                summaryCallFactorial2,
                summaryCallFactorial3
            ),
            displayNames = listOf(
                "n == 0 : True -> return 1",
                "n == 0 : False -> return n * factorial(n - 1)",
                "return r.factorial(n) : True -> ThrowIllegalArgumentException"
            )
        )
    }

    val summaryCallSimpleInvoke1 = "Test calls InvokeExample::simpleFormula,\n" +
            "    there it executes conditions:\n" +
            "        {@code (fst < 100): True}"
    val summaryCallSimpleInvoke2 = "Test calls InvokeExample::simpleFormula,\n" +
            "    there it executes conditions:\n" +
            "        {@code (fst < 100): False},\n" +
            "        {@code (snd < 100): True}"
    val summaryCallSimpleInvoke3 = "Test calls InvokeExample::simpleFormula,\n" +
            "    there it executes conditions:\n" +
            "        {@code (fst < 100): False},\n" +
            "        {@code (snd < 100): False}\n" +
            "    invokes:\n" +
            "        InvokeExample::half once,\n" +
            "        InvokeExample::mult once"

    @Test
    fun testCallSimpleInvoke() {
        checkTwoArguments(
            InnerCalls::callSimpleInvoke,
            summaryKeys = listOf(
                summaryCallSimpleInvoke1,
                summaryCallSimpleInvoke2,
                summaryCallSimpleInvoke3
            )
        )
    }

    val summaryCallStringExample1 = "Test calls StringExamples::indexOf,\n" +
            "    there it invokes:\n" +
            "        String::indexOf once"
    val summaryCallStringExample2 = "Test calls StringExamples::indexOf,\n" +
            "    there it invokes:\n" +
            "        String::indexOf once"
    val summaryCallStringExample3 = "Test calls StringExamples::indexOf,\n" +
            "    there it invokes:\n" +
            "        String::indexOf once"

    @Test
    fun testCallComplicatedMethod() {
        checkTwoArguments(
            InnerCalls::callStringExample,
            summaryKeys = listOf(
                summaryCallStringExample1,
                summaryCallStringExample2,
                summaryCallStringExample3
            )
        )
    }

    val summarySimpleSwitchCase1 = "Test calls Switch::simpleSwitch,\n" +
            "    there it activates switch case:"
    val summarySimpleSwitchCase2 = "Test calls Switch::simpleSwitch,\n" +
            "    there it activates switch case: {@code 11}"
    val summarySimpleSwitchCase3 = "Test calls Switch::simpleSwitch,\n" +
            "    there it activates switch case: {@code 12}"
    val summarySimpleSwitchCase4 = "Test calls Switch::simpleSwitch,\n" +
            "    there it activates switch case: {@code 10}"
    val summarySimpleSwitchCase5 = "Test calls Switch::simpleSwitch,\n" +
            "    there it activates switch case: {@code default}"

    @Test
    fun testCallSimpleSwitch() {
        checkOneArgument(
            InnerCalls::callSimpleSwitch,
            summaryKeys = listOf(
                summarySimpleSwitchCase1,
                summarySimpleSwitchCase2,
                summarySimpleSwitchCase3,
                summarySimpleSwitchCase4,
                summarySimpleSwitchCase5
            ),
            displayNames = listOf(
                "switch(x) case: 13 -> return 13",
                "switch(x) case: 11 -> return 12",
                "switch(x) case: 12 -> return 12",
                "switch(x) case: 10 -> return 10",
                "switch(x) case: Default -> return -1"
            )
        )
    }

    val summaryCallLookup1 = "Test calls Switch::lookupSwitch,\n" +
            "    there it activates switch case: {@code 30}"
    val summaryCallLookup2 = "Test calls Switch::lookupSwitch,\n" +
            "    there it activates switch case: {@code 10}"
    val summaryCallLookup3 = "Test calls Switch::lookupSwitch,\n" +
            "    there it activates switch case: {@code 20}"
    val summaryCallLookup4 = "Test calls Switch::lookupSwitch,\n" +
            "    there it activates switch case: {@code 0}"
    val summaryCallLookup5 = "Test calls Switch::lookupSwitch,\n" +
            "    there it activates switch case: {@code default}"

    @Test
    fun testCallLookup() {
        checkOneArgument(
            InnerCalls::callLookup,
            summaryKeys = listOf(
                summaryCallLookup1,
                summaryCallLookup2,
                summaryCallLookup3,
                summaryCallLookup4,
                summaryCallLookup5
            )
        )
    }

    val summaryDoubleSimpleInvoke1 = "Test calls InnerCalls::callSimpleInvoke,\n" +
            "    there it calls InvokeExample::simpleFormula,\n" +
            "        there it executes conditions:\n" +
            "            {@code (fst < 100): True}"
    val summaryDoubleSimpleInvoke2 = "Test calls InnerCalls::callSimpleInvoke,\n" +
            "    there it calls InvokeExample::simpleFormula,\n" +
            "        there it executes conditions:\n" +
            "            {@code (fst < 100): False},\n" +
            "            {@code (snd < 100): True}"
    val summaryDoubleSimpleInvoke3 = "Test calls InnerCalls::callSimpleInvoke,\n" +
            "    there it calls InvokeExample::simpleFormula,\n" +
            "        there it executes conditions:\n" +
            "            {@code (fst < 100): False},\n" +
            "            {@code (snd < 100): False}\n" +
            "        invokes:\n" +
            "            InvokeExample::half once,\n" +
            "            InvokeExample::mult once\n" +
            "        returns from: {@code return mult(x, y);}"

    @Test
    fun testDoubleCall() {
        checkTwoArguments(
            InnerCalls::doubleSimpleInvoke,
            summaryKeys = listOf(
                summaryDoubleSimpleInvoke1,
                summaryDoubleSimpleInvoke2,
                summaryDoubleSimpleInvoke3
            )
        )
    }

    val summaryDoubleCallLoopInsideLoop1 = "Test calls InnerCalls::callLoopInsideLoop,\n" +
            "    there it calls Cycles::loopInsideLoop,\n" +
            "        there it iterates the loop {@code for(int i = x - 5; i < x; i++)} once,\n" +
            "            inside this loop, the test executes conditions:\n" +
            "            {@code (i < 0): True}\n" +
            "        returns from: {@code return 2;}"
    val summaryDoubleCallLoopInsideLoop2 = "Test calls InnerCalls::callLoopInsideLoop,\n" +
            "    there it calls Cycles::loopInsideLoop,\n" +
            "        there it iterates the loop {@code for(int i = x - 5; i < x; i++)} once,\n" +
            "            inside this loop, the test executes conditions:\n" +
            "            {@code (i < 0): False}\n" +
            "        iterates the loop {@code for(int j = i; j < x + i; j++)} once,\n" +
            "                inside this loop, the test executes conditions:\n" +
            "            {@code (j == 7): True}\n" +
            "        returns from: {@code return 1;}"
    val summaryDoubleCallLoopInsideLoop3 = "Test calls InnerCalls::callLoopInsideLoop,\n" +
            "    there it calls Cycles::loopInsideLoop,\n" +
            "        there it does not iterate {@code for(int i = x - 5; i < x; i++)}, {@code for(int j = i; j < x + i; j++)}, returns from: {@code return -1;}"
    val summaryDoubleCallLoopInsideLoop4 = "Test calls InnerCalls::callLoopInsideLoop,\n" +
            "    there it calls Cycles::loopInsideLoop,\n" +
            "        there it iterates the loop {@code for(int i = x - 5; i < x; i++)} once,\n" +
            "            inside this loop, the test executes conditions:\n" +
            "            {@code (i < 0): False}\n" +
            "        iterates the loop {@code for(int j = i; j < x + i; j++)} 3 times,\n" +
            "                inside this loop, the test executes conditions:\n" +
            "            {@code (j == 7): False}\n" +
            "            {@code (j == 7): True}\n" +
            "        returns from: {@code return 1;}"
    val summaryDoubleCallLoopInsideLoop5 = "Test calls InnerCalls::callLoopInsideLoop,\n" +
            "    there it calls Cycles::loopInsideLoop,\n" +
            "        there it iterates the loop {@code for(int i = x - 5; i < x; i++)} 5 times."


    @Test
    fun testDoubleCallLoopInsideLoop() {
        checkOneArgument(
            InnerCalls::doubleCallLoopInsideLoop,
            summaryKeys = listOf(
                summaryDoubleCallLoopInsideLoop1,
                summaryDoubleCallLoopInsideLoop2,
                summaryDoubleCallLoopInsideLoop3,
                summaryDoubleCallLoopInsideLoop4,
                summaryDoubleCallLoopInsideLoop5,
            )
        )
    }

    val summaryCallFib1 = "Test calls Recursion::fib,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n == 0): True}"
    val summaryCallFib2 = "Test calls Recursion::fib,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n == 0): False},\n" +
            "        {@code (n == 1): True}"
    val summaryCallFib3 = "Test calls Recursion::fib,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n == 0): False},\n" +
            "        {@code (n == 1): False}\n" +
            "    triggers recursion of fib twice, returns from: {@code return fib(n - 1) + fib(n - 2);}"
    val summaryCallFib4 = "Test calls Recursion::fib,\n" +
            "    there it executes conditions:\n" +
            "        {@code (n < 0): True}\n" +
            "    triggers recursion of fib once."

    @Test
    fun testInnerCallFib() {
        checkOneArgument(
            InnerCalls::callFib,
            summaryKeys = listOf(
                summaryCallFib1,
                summaryCallFib2,
                summaryCallFib3,
                summaryCallFib4
            ),
            displayNames = listOf(
                "n == 0 : True -> return 0",
                "n == 1 : True -> return 1",
                "n == 1 : False -> return fib(n - 1) + fib(n - 2)",
                "return r.fib(n) : True -> ThrowIllegalArgumentException"
            )
        )
    }
}