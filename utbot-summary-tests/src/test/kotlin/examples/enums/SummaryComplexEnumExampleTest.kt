package examples.enums

import examples.CustomJavaDocTagsEnabler
import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.examples.enums.ComplexEnumExamples
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate

@ExtendWith(CustomJavaDocTagsEnabler::class)
class SummaryComplexEnumExampleTest : SummaryTestCaseGeneratorTest(
    ComplexEnumExamples::class
) {
    @Test
    fun testUnsafeWithField() {
        val summary1 = "@utbot.classUnderTest {@link ComplexEnumExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.enums.ComplexEnumExamples#countEqualColors(org.utbot.examples.enums.ComplexEnumExamples.Color,org.utbot.examples.enums.ComplexEnumExamples.Color,org.utbot.examples.enums.ComplexEnumExamples.Color)}\n" +
                "@utbot.executesCondition {@code (b == a): False}\n" +
                "@utbot.executesCondition {@code (c == a): False}\n" +
                "@utbot.executesCondition {@code (a == b): False}\n" +
                "@utbot.executesCondition {@code (c == b): False}\n" +
                "@utbot.executesCondition {@code (equalToA > equalToB): False}\n" +
                "@utbot.returnsFrom {@code return equalToB;}"
        val summary2 = "@utbot.classUnderTest {@link ComplexEnumExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.enums.ComplexEnumExamples#countEqualColors(org.utbot.examples.enums.ComplexEnumExamples.Color,org.utbot.examples.enums.ComplexEnumExamples.Color,org.utbot.examples.enums.ComplexEnumExamples.Color)}\n" +
                "@utbot.executesCondition {@code (b == a): False}\n" +
                "@utbot.executesCondition {@code (c == a): True}\n" +
                "@utbot.executesCondition {@code (a == b): False}\n" +
                "@utbot.executesCondition {@code (c == b): False}\n" +
                "@utbot.executesCondition {@code (equalToA > equalToB): True}\n" +
                "@utbot.returnsFrom {@code return equalToA;}\n"
        val summary3 = "@utbot.classUnderTest {@link ComplexEnumExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.enums.ComplexEnumExamples#countEqualColors(org.utbot.examples.enums.ComplexEnumExamples.Color,org.utbot.examples.enums.ComplexEnumExamples.Color,org.utbot.examples.enums.ComplexEnumExamples.Color)}\n" +
                "@utbot.executesCondition {@code (b == a): False}\n" +
                "@utbot.executesCondition {@code (c == a): False}\n" +
                "@utbot.executesCondition {@code (a == b): False}\n" +
                "@utbot.executesCondition {@code (c == b): True}\n" +
                "@utbot.executesCondition {@code (equalToA > equalToB): False}\n" +
                "@utbot.returnsFrom {@code return equalToB;}\n"
        val summary4 = "@utbot.classUnderTest {@link ComplexEnumExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.enums.ComplexEnumExamples#countEqualColors(org.utbot.examples.enums.ComplexEnumExamples.Color,org.utbot.examples.enums.ComplexEnumExamples.Color,org.utbot.examples.enums.ComplexEnumExamples.Color)}\n" +
                "@utbot.executesCondition {@code (b == a): True}\n" +
                "@utbot.executesCondition {@code (c == a): True}\n" +
                "@utbot.executesCondition {@code (a == b): True}\n" +
                "@utbot.executesCondition {@code (c == b): True}\n" +
                "@utbot.executesCondition {@code (equalToA > equalToB): False}\n" +
                "@utbot.returnsFrom {@code return equalToB;}"

        val methodName1 = "testCountEqualColors_EqualToALessOrEqualEqualToB"
        val methodName2 = "testCountEqualColors_EqualToAGreaterThanEqualToB"
        val methodName3 = "testCountEqualColors_EqualToALessOrEqualEqualToB_1"
        val methodName4 = "testCountEqualColors_AEqualsB"

        val displayName1 = "b == a : False -> return equalToB"
        val displayName2 = "equalToA > equalToB : True -> return equalToA"
        val displayName3 = "b == a : False -> return equalToB"
        val displayName4 = "b == a : True -> return equalToB"

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


        val method = ComplexEnumExamples::countEqualColors
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testFindState() {
        val summary1 = "@utbot.classUnderTest {@link ComplexEnumExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.enums.ComplexEnumExamples#findState(int)}\n" +
                "@utbot.invokes {@link org.utbot.examples.enums.State#findStateByCode(int)}\n" +
                "@utbot.returnsFrom {@code return State.findStateByCode(code);}\n"
        val summary2 = "@utbot.classUnderTest {@link ComplexEnumExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.enums.ComplexEnumExamples#findState(int)}\n" +
                "@utbot.invokes {@link org.utbot.examples.enums.State#findStateByCode(int)}\n" +
                "@utbot.returnsFrom {@code return State.findStateByCode(code);}\n"
        val summary3 = "@utbot.classUnderTest {@link ComplexEnumExamples}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.enums.ComplexEnumExamples#findState(int)}\n" +
                "@utbot.invokes {@link org.utbot.examples.enums.State#findStateByCode(int)}\n" +
                "@utbot.returnsFrom {@code return State.findStateByCode(code);}\n"

        val methodName1 = "testFindState_ReturnStateFindStateByCode"
        val methodName2 = "testFindState_ReturnStateFindStateByCode_1"
        val methodName3 = "testFindState_ReturnStateFindStateByCode_2"

        val displayName1 = "-> return State.findStateByCode(code)"
        val displayName2 = "-> return State.findStateByCode(code)"
        val displayName3 = "-> return State.findStateByCode(code)"

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


        val method = ComplexEnumExamples::findState
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}