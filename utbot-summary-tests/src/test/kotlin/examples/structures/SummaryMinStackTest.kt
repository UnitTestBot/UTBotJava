package examples.structures

import examples.CustomJavaDocTagsEnabler
import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.examples.structures.MinStack
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate
@ExtendWith(CustomJavaDocTagsEnabler::class)
class SummaryMinStackTest : SummaryTestCaseGeneratorTest(
    MinStack::class
) {
    @Test
    fun testGetMin() {
        val summary1 = "@utbot.classUnderTest {@link MinStack}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.structures.MinStack#getMin()}\n" +
                "@utbot.throwsException {@link java.lang.ArrayIndexOutOfBoundsException} in: return minStack[size - 1];"

        val summary2 = "@utbot.classUnderTest {@link MinStack}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.structures.MinStack#getMin()}\n" +
                "@utbot.throwsException {@link java.lang.NullPointerException} in: return minStack[size - 1];"

        val summary3 = "@utbot.classUnderTest {@link MinStack}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.structures.MinStack#getMin()}\n" +
                "@utbot.returnsFrom {@code return minStack[size - 1];}\n"

        val methodName1 = "testGetMin_ThrowArrayIndexOutOfBoundsException"
        val methodName2 = "testGetMin_ThrowNullPointerException"
        val methodName3 = "testGetMin_ReturnSize1OfMinStack"

        val displayName1 = "return minStack[size - 1] : True -> ThrowArrayIndexOutOfBoundsException"
        val displayName2 = "return minStack[size - 1] : True -> ThrowNullPointerException"
        val displayName3 = "-> return minStack[size - 1]"

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

        val method = MinStack::getMin
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testRemoveValue() {
        val summary1 = "@utbot.classUnderTest {@link MinStack}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.structures.MinStack#removeValue()}\n" +
                "@utbot.executesCondition {@code (size <= 0): True}\n" +
                "@utbot.throwsException {@link java.lang.RuntimeException} when: size <= 0"

        val summary2 = "@utbot.classUnderTest {@link MinStack}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.structures.MinStack#removeValue()}\n" +
                "@utbot.executesCondition {@code (size <= 0): False}\n"

        val methodName1 = "testRemoveValue_ThrowRuntimeException"
        val methodName2 = "testRemoveValue_SizeGreaterThanZero"

        val displayName1 = "size <= 0 -> ThrowRuntimeException"
        val displayName2 = "-> size <= 0 : False"

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

        val method = MinStack::removeValue
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testAddValue() {
        val summary1 = "@utbot.classUnderTest {@link MinStack}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.structures.MinStack#addValue(long)}\n" +
                "@utbot.throwsException {@link java.lang.ArrayIndexOutOfBoundsException} in: stack[size] = value;"

        val summary2 = "@utbot.classUnderTest {@link MinStack}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.structures.MinStack#addValue(long)}\n" +
                "@utbot.throwsException {@link java.lang.NullPointerException} in: stack[size] = value;"

        val summary3 = "@utbot.classUnderTest {@link MinStack}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.structures.MinStack#addValue(long)}\n" +
                "@utbot.executesCondition {@code (size == 0): True}\n" +
                "@utbot.throwsException {@link java.lang.ArrayIndexOutOfBoundsException} in: minStack[size] = value;"

        val summary4 = "@utbot.classUnderTest {@link MinStack}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.structures.MinStack#addValue(long)}\n" +
                "@utbot.executesCondition {@code (size == 0): True}\n" +
                "@utbot.throwsException {@link java.lang.NullPointerException} in: minStack[size] = value;"

        val summary5 = "@utbot.classUnderTest {@link MinStack}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.structures.MinStack#addValue(long)}\n" +
                "@utbot.executesCondition {@code (size == 0): False}\n" +
                "@utbot.throwsException {@link java.lang.NullPointerException} in: minStack[size] = Math.min(minStack[size - 1], value);"

        val summary6 = "@utbot.classUnderTest {@link MinStack}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.structures.MinStack#addValue(long)}\n" +
                "@utbot.executesCondition {@code (size == 0): False}\n" +
                "@utbot.throwsException {@link java.lang.ArrayIndexOutOfBoundsException} in: minStack[size] = Math.min(minStack[size - 1], value);"
        val summary7 = "@utbot.classUnderTest {@link MinStack}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.structures.MinStack#addValue(long)}\n" +
                "@utbot.executesCondition {@code (size == 0): False}\n" +
                "@utbot.invokes {@link java.lang.Math#min(long,long)}\n" +
                "@utbot.throwsException {@link java.lang.ArrayIndexOutOfBoundsException} in: minStack[size] = Math.min(minStack[size - 1], value);"
        val summary8 = "@utbot.classUnderTest {@link MinStack}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.structures.MinStack#addValue(long)}\n" +
                "@utbot.executesCondition {@code (size == 0): True}\n"
        val summary9 = "@utbot.classUnderTest {@link MinStack}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.structures.MinStack#addValue(long)}\n" +
                "@utbot.executesCondition {@code (size == 0): False}\n" +
                "@utbot.invokes {@link java.lang.Math#min(long,long)}\n"

        val methodName1 = "testAddValue_ThrowArrayIndexOutOfBoundsException"
        val methodName2 = "testAddValue_ThrowNullPointerException"
        val methodName3 = "testAddValue_ThrowArrayIndexOutOfBoundsException_1"
        val methodName4 = "testAddValue_ThrowNullPointerException_1"
        val methodName5 = "testAddValue_ThrowNullPointerException_2"
        val methodName6 = "testAddValue_ThrowArrayIndexOutOfBoundsException_2"
        val methodName7 = "testAddValue_ThrowArrayIndexOutOfBoundsException_3"
        val methodName8 = "testAddValue_SizeEqualsZero"
        val methodName9 = "testAddValue_SizeNotEqualsZero"

        val displayName1 = "stack[size] = value -> ThrowArrayIndexOutOfBoundsException"
        val displayName2 = "stack[size] = value -> ThrowNullPointerException"
        val displayName3 = "minStack[size] = value -> ThrowArrayIndexOutOfBoundsException"
        val displayName4 = "minStack[size] = value -> ThrowNullPointerException"
        val displayName5 = "minStack[size] = Math.min(minStack[size - 1], value) -> ThrowNullPointerException"
        val displayName6 = "minStack[size] = Math.min(minStack[size - 1], value) -> ThrowArrayIndexOutOfBoundsException"
        val displayName7 = "minStack[size] = Math.min(minStack[size - 1], value) -> ThrowArrayIndexOutOfBoundsException"
        val displayName8 = "-> size == 0 : True"
        val displayName9 = "size == 0 : False -> MathMin"

        val summaryKeys = listOf(
            summary1,
            summary2,
            summary3,
            summary4,
            summary5,
            summary6,
            summary7,
            summary8,
            summary9
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
            displayName9
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
        )

        val method = MinStack::addValue
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}