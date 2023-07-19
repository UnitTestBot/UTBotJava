package examples.inner

import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.utbot.examples.inner.InnerCalls
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate

class SummaryInnerCallsTest : SummaryTestCaseGeneratorTest(
    InnerCalls::class,
) {
    @Test
    fun testCallLoopInsideLoop() {
        val summary1 = "Test calls {@link org.utbot.examples.controlflow.Cycles#loopInsideLoop(int)},\n" +
                "    there it iterates the loop for(int i = x - 5; i < x; i++) once,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "        (i < 0): False\n" +
                "    iterates the loop for(int j = i; j < x + i; j++) once,\n" +
                "            inside this loop, the test executes conditions:\n" +
                "        (j == 7): True\n" +
                "    returns from: return 1;\n" +
                "Test afterwards returns from: return cycles.loopInsideLoop(x);\n"
        val summary2 = "Test calls {@link org.utbot.examples.controlflow.Cycles#loopInsideLoop(int)},\n" +
                "    there it iterates the loop for(int i = x - 5; i < x; i++) once,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "        (i < 0): True\n" +
                "    returns from: return 2;\n" +
                "Test then returns from: return cycles.loopInsideLoop(x);\n"
        val summary3 = "Test calls {@link org.utbot.examples.controlflow.Cycles#loopInsideLoop(int)},\n" +
                "    there it does not iterate for(int i = x - 5; i < x; i++), for(int j = i; j < x + i; j++), returns from: return -1;\n" +
                "    \n" +
                "Test later returns from: return cycles.loopInsideLoop(x);\n"
        val summary4 = "Test calls {@link org.utbot.examples.controlflow.Cycles#loopInsideLoop(int)},\n" +
                "    there it iterates the loop for(int i = x - 5; i < x; i++) once,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "        (i < 0): False\n" +
                "    iterates the loop for(int j = i; j < x + i; j++) twice,\n" +
                "            inside this loop, the test executes conditions:\n" +
                "        (j == 7): False\n" +
                "        (j == 7): True\n" +
                "    returns from: return 1;\n" +
                "Test later returns from: return cycles.loopInsideLoop(x);\n"
        val summary5 = "Test calls {@link org.utbot.examples.controlflow.Cycles#loopInsideLoop(int)},\n" +
                "    there it iterates the loop for(int i = x - 5; i < x; i++) 5 times. \n" +
                "    Test further does not iterate for(int j = i; j < x + i; j++), returns from: return -1;\n" +
                "    \n" +
                "Test then returns from: return cycles.loopInsideLoop(x);\n"

        val methodName1 = "testCallLoopInsideLoop_JEquals7"
        val methodName2 = "testCallLoopInsideLoop_ILessThanZero"
        val methodName3 = "testCallLoopInsideLoop_ReturnNegative1"
        val methodName4 = "testCallLoopInsideLoop_JNotEquals7"
        val methodName5 = "testCallLoopInsideLoop_ReturnNegative1_1"


        val displayName1 = "i < 0 : False -> return 1"
        val displayName2 = "i < 0 : True -> return 2"
        val displayName3 = "loopInsideLoop -> return -1"
        val displayName4 = "j == 7 : False -> return 1"
        val displayName5 = "loopInsideLoop -> return -1"


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

        val method = InnerCalls::callLoopInsideLoop
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testCallLeftBinSearch() {
        //NOTE: 5 and 6 cases has different paths but throws the equal exception.
        val summary1 = "Test calls {@link org.utbot.examples.algorithms.BinarySearch#leftBinSearch(long[],long)},\n" +
                "    there it does not iterate while(left < right - 1), executes conditions:\n" +
                "        (found): False\n" +
                "    returns from: return -1;\n" +
                "    \n" +
                "Test then returns from: return binarySearch.leftBinSearch(array, key);\n"
        val summary2 = "Test calls {@link org.utbot.examples.algorithms.BinarySearch#leftBinSearch(long[],long)},\n" +
                "    there it iterates the loop while(left < right - 1) once,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "        (array[middle] == key): False,\n" +
                "        (array[middle] < key): True\n" +
                "    Test afterwards executes conditions:\n" +
                "        (found): False\n" +
                "    returns from: return -1;\n" +
                "    \n" +
                "Test next returns from: return binarySearch.leftBinSearch(array, key);\n"
        val summary3 = "Test calls {@link org.utbot.examples.algorithms.BinarySearch#leftBinSearch(long[],long)},\n" +
                "    there it iterates the loop while(left < right - 1) once,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "        (array[middle] == key): False,\n" +
                "        (array[middle] < key): False\n" +
                "    Test afterwards executes conditions:\n" +
                "        (found): False\n" +
                "    returns from: return -1;\n" +
                "    \n" +
                "Test next returns from: return binarySearch.leftBinSearch(array, key);\n"
        val summary4 = "Test calls {@link org.utbot.examples.algorithms.BinarySearch#leftBinSearch(long[],long)},\n" +
                "    there it iterates the loop while(left < right - 1) once,\n" +
                "        inside this loop, the test executes conditions:\n" +
                "        (array[middle] == key): True,\n" +
                "        (array[middle] < key): False\n" +
                "    Test then executes conditions:\n" +
                "        (found): True\n" +
                "    returns from: return right + 1;\n" +
                "    \n" +
                "Test further returns from: return binarySearch.leftBinSearch(array, key);\n"
        val summary5 = "Test \n" +
                "throws IllegalArgumentException in: return binarySearch.leftBinSearch(array, key);\n"
        val summary6 = "Test \n" +
                "throws IllegalArgumentException in: return binarySearch.leftBinSearch(array, key);\n"
        val summary7 = "Test calls {@link org.utbot.examples.algorithms.BinarySearch#leftBinSearch(long[],long)},\n" +
                "    there it invokes:\n" +
                "        org.utbot.examples.algorithms.BinarySearch#isUnsorted(long[]) once\n" +
                "    triggers recursion of leftBinSearch once, \n" +
                "Test throws NullPointerException in: return binarySearch.leftBinSearch(array, key);\n"

        val methodName1 = "testCallLeftBinSearch_NotFound"
        val methodName2 = "testCallLeftBinSearch_MiddleOfArrayLessThanKey"
        val methodName3 = "testCallLeftBinSearch_NotFound_1"
        val methodName4 = "testCallLeftBinSearch_Found"
        val methodName5 = "testCallLeftBinSearch_ThrowIllegalArgumentException"
        val methodName6 = "testCallLeftBinSearch_ThrowIllegalArgumentException_1"
        val methodName7 = "testCallLeftBinSearch_ThrowNullPointerException"


        val displayName1 = "found : False -> return -1"
        val displayName2 = "array[middle] < key : True -> return -1"
        val displayName3 = "while(left < right - 1) -> return -1"
        val displayName4 = "array[middle] == key : True -> return right + 1"
        val displayName5 =
            "return binarySearch.leftBinSearch(array, key) : True -> ThrowIllegalArgumentException" // TODO: probably return statement could be removed
        val displayName6 = "return binarySearch.leftBinSearch(array, key) : True -> ThrowIllegalArgumentException"
        val displayName7 = "return binarySearch.leftBinSearch(array, key) : True -> ThrowNullPointerException"


        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3,
            summary4,
            summary5,
            summary6,
            summary7
        )

        val displayNames = listOf(
            displayName1,
            displayName2,
            displayName3,
            displayName4,
            displayName5,
            displayName6,
            displayName7
        )

        val methodNames = listOf(
            methodName1,
            methodName2,
            methodName3,
            methodName4,
            methodName5,
            methodName6,
            methodName7
        )

        val method = InnerCalls::callLeftBinSearch
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    // TODO: SAT-1211
    @Test
    fun testCallCreateNewThreeDimensionalArray() {
        val summary1 =
            "Test calls {@link org.utbot.examples.arrays.ArrayOfArrays#createNewThreeDimensionalArray(int,int)},\n" +
                    "    there it executes conditions:\n" +
                    "        (length != 2): True\n" +
                    "    returns from: return new int[0][][];\n" +
                    "    "
        val summary2 =
            "Test calls {@link org.utbot.examples.arrays.ArrayOfArrays#createNewThreeDimensionalArray(int,int)},\n" +
                    "    there it executes conditions:\n" +
                    "        (length != 2): False\n" +
                    "    iterates the loop for(int i = 0; i < length; i++) once,\n" +
                    "        inside this loop, the test iterates the loop for(int j = 0; j < length; j++) once,\n" +
                    "            inside this loop, the test iterates the loop for(int k = 0; k < length; k++)\n" +
                    "    Test then returns from: return matrix;\n" +
                    "    "

        val methodName1 = "testCallCreateNewThreeDimensionalArray_LengthNotEquals2"
        val methodName2 = "testCallCreateNewThreeDimensionalArray_LengthEquals2"

        val displayName1 = "length != 2 : True -> return new int[0][][]"
        val displayName2 = "length != 2 : False -> return matrix"

        val summaryKeys = listOf(
            summary1,
            summary2
        )

        val displayNames = listOf(
            displayName1,
            displayName2
        )

        val methodNames = listOf(
            methodName1,
            methodName2
        )

        val method = InnerCalls::callCreateNewThreeDimensionalArray
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testCallInitExamples() {
        // NOTE: paths are different for test cases 1 and 2
        val summary1 = "Test calls {@link org.utbot.examples.exceptions.ExceptionExamples#initAnArray(int)},\n" +
                "    there it catches exception:\n" +
                "        IndexOutOfBoundsException e\n" +
                "    returns from: return -3;\n" +
                "    \n" +
                "Test later returns from: return exceptionExamples.initAnArray(n);\n"
        val summary2 = "Test calls {@link org.utbot.examples.exceptions.ExceptionExamples#initAnArray(int)},\n" +
                "    there it catches exception:\n" +
                "        IndexOutOfBoundsException e\n" +
                "    returns from: return -3;\n" +
                "    \n" +
                "Test then returns from: return exceptionExamples.initAnArray(n);\n"
        val summary3 = "Test calls {@link org.utbot.examples.exceptions.ExceptionExamples#initAnArray(int)},\n" +
                "    there it catches exception:\n" +
                "        NegativeArraySizeException e\n" +
                "    returns from: return -2;\n" +
                "    \n" +
                "Test next returns from: return exceptionExamples.initAnArray(n);\n"
        val summary4 = "Test calls {@link org.utbot.examples.exceptions.ExceptionExamples#initAnArray(int)},\n" +
                "    there it returns from: return a[n - 1] + a[n - 2];\n" +
                "    \n" +
                "Test afterwards returns from: return exceptionExamples.initAnArray(n);\n"

        val methodName1 = "testCallInitExamples_CatchIndexOutOfBoundsException"
        val methodName2 = "testCallInitExamples_CatchIndexOutOfBoundsException_1"
        val methodName3 = "testCallInitExamples_CatchNegativeArraySizeException"
        val methodName4 = "testCallInitExamples_ReturnN1OfAPlusN2OfA"

        val displayName1 = "Catch (IndexOutOfBoundsException e) -> return -3"
        val displayName2 = "Catch (IndexOutOfBoundsException e) -> return -3"
        val displayName3 = "Catch (NegativeArraySizeException e) -> return -2"
        val displayName4 = "initAnArray -> return a[n - 1] + a[n - 2]"

        val method = InnerCalls::callInitExamples
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testCallFactorial() {
        val summary1 = "Test calls {@link org.utbot.examples.recursion.Recursion#factorial(int)},\n" +
                "    there it executes conditions:\n" +
                "        (n == 0): True\n" +
                "    returns from: return 1;\n" +
                "    \n" +
                "Test next returns from: return r.factorial(n);\n"
        val summary2 = "Test calls {@link org.utbot.examples.recursion.Recursion#factorial(int)},\n" +
                "    there it executes conditions:\n" +
                "        (n == 0): False\n" +
                "    triggers recursion of factorial once, returns from: return n * factorial(n - 1);\n" +
                "    \n" +
                "Test further returns from: return r.factorial(n);\n"
        val summary3 = "Test calls {@link org.utbot.examples.recursion.Recursion#factorial(int)},\n" +
                "    there it executes conditions:\n" +
                "        (n < 0): True\n" +
                "    triggers recursion of factorial once, \n" +
                "Test throws IllegalArgumentException in: return r.factorial(n);\n"

        val methodName1 = "testCallFactorial_ThrowIllegalArgumentException"
        val methodName2 = "testCallFactorial_NNotEqualsZero"
        val methodName3 = "testCallFactorial_NEqualsZero"

        val displayName1 = "n == 0 : True -> return 1"
        val displayName2 = "n == 0 : False -> return n * factorial(n - 1)"
        val displayName3 = "return r.factorial(n) : True -> ThrowIllegalArgumentException"

        val method = InnerCalls::callFactorial
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testCallSimpleInvoke() {
        val summary1 = "Test calls {@link org.utbot.examples.invokes.InvokeExample#simpleFormula(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (fst < 100): False,\n" +
                "        (snd < 100): True\n" +
                "    \n" +
                "Test throws IllegalArgumentException in: return invokeExample.simpleFormula(f, s);\n"
        val summary2 = "Test calls {@link org.utbot.examples.invokes.InvokeExample#simpleFormula(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (fst < 100): True\n" +
                "    \n" +
                "Test throws IllegalArgumentException in: return invokeExample.simpleFormula(f, s);\n"
        val summary3 = "Test calls {@link org.utbot.examples.invokes.InvokeExample#simpleFormula(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (fst < 100): False,\n" +
                "        (snd < 100): False\n" +
                "    invokes:\n" +
                "        org.utbot.examples.invokes.InvokeExample#half(int) once,\n" +
                "        org.utbot.examples.invokes.InvokeExample#mult(int,int) once\n" +
                "    returns from: return mult(x, y);\n" +
                "    \n" +
                "Test then returns from: return invokeExample.simpleFormula(f, s);\n"

        val methodName1 = "testCallSimpleInvoke_ThrowIllegalArgumentException"
        val methodName2 = "testCallSimpleInvoke_ThrowIllegalArgumentException_1"
        val methodName3 = "testCallSimpleInvoke_SndGreaterOrEqual100"

        val displayName1 = "return invokeExample.simpleFormula(f, s) : True -> ThrowIllegalArgumentException"
        val displayName2 = "return invokeExample.simpleFormula(f, s) : True -> ThrowIllegalArgumentException"
        val displayName3 = "fst < 100 : True -> return mult(x, y)"

        val method = InnerCalls::callSimpleInvoke
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testCallComplicatedMethod() {
        val summary1 =
            "Test calls {@link org.utbot.examples.strings.StringExamples#indexOf(java.lang.String,java.lang.String)},\n" +
                    "    there it invokes:\n" +
                    "        {@link java.lang.String#indexOf(java.lang.String)} once\n" +
                    "    triggers recursion of indexOf once, \n" +
                    "Test throws NullPointerException in: return stringExamples.indexOf(s, key);\n"
        val summary2 =
            "Test calls {@link org.utbot.examples.strings.StringExamples#indexOf(java.lang.String,java.lang.String)},\n" +
                    "    there it invokes:\n" +
                    "        {@link java.lang.String#indexOf(java.lang.String)} once\n" +
                    "    \n" +
                    "Test throws NullPointerException \n"
        val summary3 =
            "Test calls {@link org.utbot.examples.strings.StringExamples#indexOf(java.lang.String,java.lang.String)},\n" +
                    "    there it executes conditions:\n" +
                    "        (i > 0): False,\n" +
                    "        (i == 0): True\n" +
                    "    returns from: return i;\n" +
                    "    \n" +
                    "Test further returns from: return stringExamples.indexOf(s, key);\n"
        val summary4 =
            "Test calls {@link org.utbot.examples.strings.StringExamples#indexOf(java.lang.String,java.lang.String)},\n" +
                    "    there it executes conditions:\n" +
                    "        (i > 0): False,\n" +
                    "        (i == 0): False\n" +
                    "    returns from: return i;\n" +
                    "    \n" +
                    "Test later returns from: return stringExamples.indexOf(s, key);\n"
        val summary5 =
            "Test calls {@link org.utbot.examples.strings.StringExamples#indexOf(java.lang.String,java.lang.String)},\n" +
                    "    there it executes conditions:\n" +
                    "        (i > 0): True\n" +
                    "    returns from: return i;\n" +
                    "    \n" +
                    "Test afterwards returns from: return stringExamples.indexOf(s, key);\n"

        val methodName1 = "testCallStringExample_ThrowNullPointerException"
        val methodName2 = "testCallStringExample_ThrowNullPointerException_1"
        val methodName3 = "testCallStringExample_IEqualsZero"
        val methodName4 = "testCallStringExample_INotEqualsZero"
        val methodName5 = "testCallStringExample_IGreaterThanZero"

        val displayName1 = "return stringExamples.indexOf(s, key) : True -> ThrowNullPointerException"
        val displayName2 = " -> ThrowNullPointerException"
        val displayName3 = "i == 0 : True -> return i"
        val displayName4 = "i == 0 : False -> return i"
        val displayName5 = "i > 0 : True -> return i"

        val method = InnerCalls::callStringExample
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testCallSimpleSwitch() {
        val summary1 = "Test calls {@link org.utbot.examples.controlflow.Switch#simpleSwitch(int)},\n" +
                "    there it activates switch(x) case: 12, returns from: return 12;\n" +
                "    "
        val summary2 = "Test calls {@link org.utbot.examples.controlflow.Switch#simpleSwitch(int)},\n" +
                "    there it activates switch(x) case: 13, returns from: return 13;\n" +
                "    "
        val summary3 = "Test calls {@link org.utbot.examples.controlflow.Switch#simpleSwitch(int)},\n" +
                "    there it activates switch(x) case: 10, returns from: return 10;\n" +
                "    "
        val summary4 = "Test calls {@link org.utbot.examples.controlflow.Switch#simpleSwitch(int)},\n" +
                "    there it activates switch(x) case: default, returns from: return -1;\n" +
                "    "

        val methodName1 = "testCallSimpleSwitch_Return12"
        val methodName2 = "testCallSimpleSwitch_Return13"
        val methodName3 = "testCallSimpleSwitch_Return10"
        val methodName4 = "testCallSimpleSwitch_ReturnNegative1"

        val displayName1 = "switch(x) case: 12 -> return 12"
        val displayName2 = "switch(x) case: 13 -> return 13"
        val displayName3 = "switch(x) case: 10 -> return 10"
        val displayName4 = "switch(x) case: default -> return -1"

        val method = InnerCalls::callSimpleSwitch
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testCallLookup() {
        val summary1 = "Test calls {@link org.utbot.examples.controlflow.Switch#lookupSwitch(int)},\n" +
                "    there it activates switch(x) case: 20, returns from: return 20;\n" +
                "    "
        val summary2 = "Test calls {@link org.utbot.examples.controlflow.Switch#lookupSwitch(int)},\n" +
                "    there it activates switch(x) case: 30, returns from: return 30;\n" +
                "    "
        val summary3 = "Test calls {@link org.utbot.examples.controlflow.Switch#lookupSwitch(int)},\n" +
                "    there it activates switch(x) case: 0, returns from: return 0;\n" +
                "    "
        val summary4 = "Test calls {@link org.utbot.examples.controlflow.Switch#lookupSwitch(int)},\n" +
                "    there it activates switch(x) case: default, returns from: return -1;\n" +
                "    "

        val methodName1 = "testCallLookup_Return20"
        val methodName2 = "testCallLookup_Return30"
        val methodName3 = "testCallLookup_ReturnZero"
        val methodName4 = "testCallLookup_ReturnNegative1"

        val displayName1 = "switch(x) case: 20 -> return 20"
        val displayName2 = "switch(x) case: 30 -> return 30"
        val displayName3 = "switch(x) case: 0 -> return 0"
        val displayName4 = "switch(x) case: default -> return -1"

        val method = InnerCalls::callLookup
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testDoubleCall() {
        val summary1 = "Test calls {@link org.utbot.examples.inner.InnerCalls#callSimpleInvoke(int,int)},\n" +
                "    there it calls {@link org.utbot.examples.invokes.InvokeExample#simpleFormula(int,int)},\n" +
                "        there it executes conditions:\n" +
                "            (fst < 100): True\n" +
                "        \n" +
                "Test throws IllegalArgumentException in: callSimpleInvoke(f, s);\n"
        val summary2 = "Test calls {@link org.utbot.examples.inner.InnerCalls#callSimpleInvoke(int,int)},\n" +
                "    there it calls {@link org.utbot.examples.invokes.InvokeExample#simpleFormula(int,int)},\n" +
                "        there it executes conditions:\n" +
                "            (fst < 100): False,\n" +
                "            (snd < 100): True\n" +
                "        \n" +
                "Test throws IllegalArgumentException in: callSimpleInvoke(f, s);\n"
        val summary3 = "Test calls {@link org.utbot.examples.inner.InnerCalls#callSimpleInvoke(int,int)},\n" +
                "    there it calls {@link org.utbot.examples.invokes.InvokeExample#simpleFormula(int,int)},\n" +
                "        there it executes conditions:\n" +
                "            (fst < 100): False,\n" +
                "            (snd < 100): False\n" +
                "        invokes:\n" +
                "            org.utbot.examples.invokes.InvokeExample#half(int) once,\n" +
                "            org.utbot.examples.invokes.InvokeExample#mult(int,int) once\n" +
                "        returns from: return mult(x, y);\n" +
                "        \n" +
                "    Test later returns from: return invokeExample.simpleFormula(f, s);\n" +
                "    "

        val methodName1 = "testDoubleSimpleInvoke_ThrowIllegalArgumentException"
        val methodName2 = "testDoubleSimpleInvoke_ThrowIllegalArgumentException_1"
        val methodName3 = "testDoubleSimpleInvoke_SndGreaterOrEqual100"

        val displayName1 = "callSimpleInvoke(f, s) : True -> ThrowIllegalArgumentException"
        val displayName2 = "callSimpleInvoke(f, s) : True -> ThrowIllegalArgumentException"
        val displayName3 = "fst < 100 : True -> return mult(x, y)"

        val method = InnerCalls::doubleSimpleInvoke
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testDoubleCallLoopInsideLoop() {
        val summary1 = "Test calls {@link org.utbot.examples.inner.InnerCalls#callLoopInsideLoop(int)},\n" +
                "    there it calls {@link org.utbot.examples.controlflow.Cycles#loopInsideLoop(int)},\n" +
                "        there it iterates the loop for(int i = x - 5; i < x; i++) once,\n" +
                "            inside this loop, the test executes conditions:\n" +
                "            (i < 0): True\n" +
                "        returns from: return 2;\n" +
                "    Test afterwards returns from: return cycles.loopInsideLoop(x);\n" +
                "    \n" +
                "Test further returns from: return result;\n"
        val summary2 = "Test calls {@link org.utbot.examples.inner.InnerCalls#callLoopInsideLoop(int)},\n" +
                "    there it calls {@link org.utbot.examples.controlflow.Cycles#loopInsideLoop(int)},\n" +
                "        there it does not iterate for(int i = x - 5; i < x; i++), for(int j = i; j < x + i; j++), returns from: return -1;\n" +
                "        \n" +
                "    Test next returns from: return cycles.loopInsideLoop(x);\n" +
                "    \n" +
                "Test later returns from: return result;\n"
        val summary3 = "Test calls {@link org.utbot.examples.inner.InnerCalls#callLoopInsideLoop(int)},\n" +
                "    there it calls {@link org.utbot.examples.controlflow.Cycles#loopInsideLoop(int)},\n" +
                "        there it iterates the loop for(int i = x - 5; i < x; i++) once,\n" +
                "            inside this loop, the test executes conditions:\n" +
                "            (i < 0): False\n" +
                "        iterates the loop for(int j = i; j < x + i; j++) once,\n" +
                "                inside this loop, the test executes conditions:\n" +
                "            (j == 7): True\n" +
                "        returns from: return 1;\n" +
                "    Test next returns from: return cycles.loopInsideLoop(x);\n" +
                "    \n" +
                "Test further returns from: return result;\n"
        val summary4 = "Test calls {@link org.utbot.examples.inner.InnerCalls#callLoopInsideLoop(int)},\n" +
                "    there it calls {@link org.utbot.examples.controlflow.Cycles#loopInsideLoop(int)},\n" +
                "        there it iterates the loop for(int i = x - 5; i < x; i++) once,\n" +
                "            inside this loop, the test executes conditions:\n" +
                "            (i < 0): False\n" +
                "        iterates the loop for(int j = i; j < x + i; j++) twice,\n" +
                "                inside this loop, the test executes conditions:\n" +
                "            (j == 7): False\n" +
                "            (j == 7): True\n" +
                "        returns from: return 1;\n" +
                "    Test further returns from: return cycles.loopInsideLoop(x);\n" +
                "    \n" +
                "Test then returns from: return result;\n"
        val summary5 = "Test calls {@link org.utbot.examples.inner.InnerCalls#callLoopInsideLoop(int)},\n" +
                "    there it calls {@link org.utbot.examples.controlflow.Cycles#loopInsideLoop(int)},\n" +
                "        there it iterates the loop for(int i = x - 5; i < x; i++) 5 times. \n" +
                "        Test later does not iterate for(int j = i; j < x + i; j++), returns from: return -1;\n" +
                "        \n" +
                "    Test afterwards returns from: return cycles.loopInsideLoop(x);\n" +
                "    \n" +
                "Test then returns from: return result;\n"

        val methodName1 = "testDoubleCallLoopInsideLoop_ILessThanZero"
        val methodName2 = "testDoubleCallLoopInsideLoop_ReturnNegative1"
        val methodName3 = "testDoubleCallLoopInsideLoop_JEquals7"
        val methodName4 = "testDoubleCallLoopInsideLoop_JNotEquals7"
        val methodName5 = "testDoubleCallLoopInsideLoop_ReturnNegative1_1"

        val displayName1 = "i < 0 : True -> return 2"
        val displayName2 = "loopInsideLoop -> return -1"
        val displayName3 = "i < 0 : False -> return 1"
        val displayName4 = "j == 7 : False -> return 1"
        val displayName5 = "loopInsideLoop -> return -1"

        val method = InnerCalls::doubleCallLoopInsideLoop
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testInnerCallFib() {
        val summary1 = "Test calls {@link org.utbot.examples.recursion.Recursion#fib(int)},\n" +
                "    there it executes conditions:\n" +
                "        (n == 0): False,\n" +
                "        (n == 1): True\n" +
                "    returns from: return 1;\n" +
                "    \n" +
                "Test next returns from: return r.fib(n);\n"
        val summary2 = "Test calls {@link org.utbot.examples.recursion.Recursion#fib(int)},\n" +
                "    there it executes conditions:\n" +
                "        (n == 0): True\n" +
                "    returns from: return 0;\n" +
                "    \n" +
                "Test next returns from: return r.fib(n);\n"
        val summary3 = "Test calls {@link org.utbot.examples.recursion.Recursion#fib(int)},\n" +
                "    there it executes conditions:\n" +
                "        (n == 0): False,\n" +
                "        (n == 1): False\n" +
                "    triggers recursion of fib twice, returns from: return fib(n - 1) + fib(n - 2);\n" +
                "    \n" +
                "Test next returns from: return r.fib(n);\n"
        val summary4 = "Test calls {@link org.utbot.examples.recursion.Recursion#fib(int)},\n" +
                "    there it executes conditions:\n" +
                "        (n < 0): True\n" +
                "    triggers recursion of fib once, \n" +
                "Test throws IllegalArgumentException in: return r.fib(n);\n"

        val methodName1 = "testCallFib_NEquals1"
        val methodName2 = "testCallFib_ThrowIllegalArgumentException"
        val methodName3 = "testCallFib_NNotEquals1"
        val methodName4 = "testCallFib_NEqualsZero"

        val displayName1 = "n == 1 : True -> return 1"
        val displayName2 = "n == 0 : True -> return 0"
        val displayName3 = "n == 1 : False -> return fib(n - 1) + fib(n - 2)"
        val displayName4 = "return r.fib(n) : True -> ThrowIllegalArgumentException"

        val method = InnerCalls::callFib
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}