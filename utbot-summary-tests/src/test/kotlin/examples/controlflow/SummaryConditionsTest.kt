package examples.controlflow

import examples.CustomJavaDocTagsEnabler
import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.testing.DoNotCalculate
import org.utbot.examples.controlflow.Conditions
import org.utbot.framework.plugin.api.MockStrategyApi

@ExtendWith(CustomJavaDocTagsEnabler::class)
class SummaryConditionsTest : SummaryTestCaseGeneratorTest(
    Conditions::class
) {
    @Test
    fun testSimpleCondition() {
        val summary1 = "@utbot.classUnderTest {@link Conditions}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Conditions#simpleCondition(boolean)}\n" +
                "@utbot.executesCondition {@code (condition): False}\n" +
                "@utbot.returnsFrom {@code return 0;}"

        val summary2 = "@utbot.classUnderTest {@link Conditions}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Conditions#simpleCondition(boolean)}\n" +
                "@utbot.executesCondition {@code (condition): True}\n" +
                "@utbot.returnsFrom {@code return 1;}"

        val methodName1 = "testSimpleCondition_NotCondition"
        val methodName2 = "testSimpleCondition_Condition"

        val displayName1 = "condition : False -> return 0"
        val displayName2 = "condition : True -> return 1"

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

        val method = Conditions::simpleCondition
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testReturnCastFromTernaryOperator() {
        val summary1 = "@utbot.classUnderTest {@link Conditions}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Conditions#returnCastFromTernaryOperator(long,int)}\n" +
                "@utbot.returnsFrom {@code return (int) (a < 0 ? a + b : a);}\n"
        val summary2 = "@utbot.classUnderTest {@link Conditions}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Conditions#returnCastFromTernaryOperator(long,int)}\n" +
                "@utbot.returnsFrom {@code return (int) (a < 0 ? a + b : a);}\n"
        val summary3 = "@utbot.classUnderTest {@link Conditions}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.controlflow.Conditions#returnCastFromTernaryOperator(long,int)}\n" +
                "@utbot.throwsException {@link java.lang.ArithmeticException} in: a = a % b;\n"

        val methodName1 = "testReturnCastFromTernaryOperator_A0aba"
        val methodName2 = "testReturnCastFromTernaryOperator_A0aba_1"
        val methodName3 = "testReturnCastFromTernaryOperator_ThrowArithmeticException"

        val displayName1 = "return (int) (a < 0 ? a + b : a) : False -> return (int) (a < 0 ? a + b : a)"
        val displayName2 = "return (int) (a < 0 ? a + b : a) : True -> return (int) (a < 0 ? a + b : a)"
        val displayName3 = "a = a % b -> ThrowArithmeticException"

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

        val method = Conditions::returnCastFromTernaryOperator
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}