package examples.ternary

import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.utbot.examples.ternary.Ternary
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate
class SummaryTernaryTest : SummaryTestCaseGeneratorTest(
    Ternary::class,
) {
    @Test
    fun testMax() {
        val summary1 = "Test executes conditions:\n" +
                "    (val1 >= val2): False\n" +
                "returns from: return val1 >= val2 ? val1 : val2;\n"
        val summary2 = "Test executes conditions:\n" +
                "    (val1 >= val2): True\n" +
                "returns from: return val1 >= val2 ? val1 : val2;\n"

        val methodName1 = "testMax_Val1LessThanVal2"
        val methodName2 = "testMax_Val1GreaterOrEqualVal2"

        val displayName1 = "val1 >= val2 : False -> return val1 >= val2 ? val1 : val2"
        val displayName2 = "val1 >= val2 : True -> return val1 >= val2 ? val1 : val2"

        val method = Ternary::max
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testSimpleOperation() {
        val summary1 = "Test returns from: return result;\n"

        val methodName1 = "testSimpleOperation_ReturnResult"

        val displayName1 = "-> return result"

        val method = Ternary::simpleOperation
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        val summaryKeys = listOf(
            summary1
        )

        val displayNames = listOf(
            displayName1
        )

        val methodNames = listOf(
            methodName1
        )

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testStringExpr() {
        val summary1 = "Test executes conditions:\n" +
                "    (num > 10): True\n" +
                "returns from: return num > 10 ? \"Number is greater than 10\" : num > 5 ? \"Number is greater than 5\" : \"Number is less than equal to 5\";\n"
        val summary2 = "Test executes conditions:\n" +
                "    (num > 10): False,\n" +
                "    (num > 5): False\n" +
                "returns from: return num > 10 ? \"Number is greater than 10\" : num > 5 ? \"Number is greater than 5\" : \"Number is less than equal to 5\";\n"
        val summary3 = "Test executes conditions:\n" +
                "    (num > 10): False,\n" +
                "    (num > 5): True\n" +
                "returns from: return num > 10 ? \"Number is greater than 10\" : num > 5 ? \"Number is greater than 5\" : \"Number is less than equal to 5\";\n"

        val methodName1 = "testStringExpr_NumGreaterThan10"
        val methodName2 = "testStringExpr_NumLessOrEqual5"
        val methodName3 = "testStringExpr_NumGreaterThan5"

        val displayName1 =
            "num > 10 : True -> return num > 10 ? \"Number is greater than 10\" : num > 5 ? \"Number is greater than 5\" : \"Number is less than equal to 5\""
        val displayName2 =
            "num > 5 : False -> return num > 10 ? \"Number is greater than 10\" : num > 5 ? \"Number is greater than 5\" : \"Number is less than equal to 5\""
        val displayName3 =
            "num > 5 : True -> return num > 10 ? \"Number is greater than 10\" : num > 5 ? \"Number is greater than 5\" : \"Number is less than equal to 5\""

        val method = Ternary::stringExpr
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
    fun testParse() {
        val summary1 = "Test executes conditions:\n" +
                "    (input == null || input.equals(\"\")): False\n" +
                "returns from: return value;\n"
        val summary2 = "Test executes conditions:\n" +
                "    (input == null || input.equals(\"\")): True\n" +
                "invokes:\n" +
                "    {@link java.lang.String#equals(java.lang.Object)} once\n" +
                "returns from: return value;\n"
        val summary3 = "Test executes conditions:\n" +
                "    (input == null || input.equals(\"\")): True\n" +
                "invokes:\n" +
                "    {@link java.lang.String#equals(java.lang.Object)} once\n" +
                "executes conditions:\n" +
                "    (input == null || input.equals(\"\")): False\n" +
                "invokes:\n" +
                "    {@link java.lang.Integer#parseInt(java.lang.String)} once\n" +
                "throws NumberFormatException in: Integer.parseInt(input)"

        val methodName1 = "testParse_InputEqualsNullOrInputEquals"
        val methodName2 = "testParse_InputNotEqualsNullOrInputEquals"
        val methodName3 = "testParse_ThrowNumberFormatException"

        val displayName1 = "input == null || input.equals(\"\") : False -> return value"
        val displayName2 = "input == null || input.equals(\"\") : True -> return value"
        val displayName3 = "Integer.parseInt(input) : True -> ThrowNumberFormatException"

        val method = Ternary::parse
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
    fun testMinValue() {
        val summary1 = "Test executes conditions:\n" +
                "    ((a < b)): False\n" +
                "returns from: return (a < b) ? a : b;\n"
        val summary2 = "Test executes conditions:\n" +
                "    ((a < b)): True\n" +
                "returns from: return (a < b) ? a : b;\n"

        val methodName1 = "testMinValue_AGreaterOrEqualB"
        val methodName2 = "testMinValue_ALessThanB"

        val displayName1 = "a < b : False -> return (a < b) ? a : b"
        val displayName2 = "a < b : True -> return (a < b) ? a : b"

        val method = Ternary::minValue
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testSubDelay() {
        val summary1 = "Test executes conditions:\n" +
                "    (flag): False\n" +
                "returns from: return flag ? 100 : 0;\n"
        val summary2 = "Test executes conditions:\n" +
                "    (flag): True\n" +
                "returns from: return flag ? 100 : 0;\n"

        val methodName1 = "testSubDelay_NotFlag"
        val methodName2 = "testSubDelay_Flag"

        val displayName1 = "flag : False -> return flag ? 100 : 0"
        val displayName2 = "flag : True -> return flag ? 100 : 0"

        val method = Ternary::subDelay
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testPlusOrMinus() {
        val summary1 = "Test executes conditions:\n" +
                "    ((num1 > num2)): False\n" +
                "returns from: return (num1 > num2) ? (num1 + num2) : (num1 - num2);\n"
        val summary2 = "Test executes conditions:\n" +
                "    ((num1 > num2)): True\n" +
                "returns from: return (num1 > num2) ? (num1 + num2) : (num1 - num2);\n"

        val methodName1 = "testPlusOrMinus_Num1LessOrEqualNum2"
        val methodName2 = "testPlusOrMinus_Num1GreaterThanNum2"

        val displayName1 = "num1 > num2 : False -> return (num1 > num2) ? (num1 + num2) : (num1 - num2)"
        val displayName2 = "num1 > num2 : True -> return (num1 > num2) ? (num1 + num2) : (num1 - num2)"

        val method = Ternary::plusOrMinus
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testLongTernary() {
        val summary1 = "Test executes conditions:\n" +
                "    (num1 > num2): True\n" +
                "returns from: return num1 > num2 ? 1 : num1 == num2 ? 2 : 3;\n"
        val summary2 = "Test executes conditions:\n" +
                "    (num1 > num2): False,\n" +
                "    (num1 == num2): True\n" +
                "returns from: return num1 > num2 ? 1 : num1 == num2 ? 2 : 3;\n"
        val summary3 = "Test executes conditions:\n" +
                "    (num1 > num2): False,\n" +
                "    (num1 == num2): False\n" +
                "returns from: return num1 > num2 ? 1 : num1 == num2 ? 2 : 3;\n"

        val methodName1 = "testLongTernary_Num1GreaterThanNum2"
        val methodName2 = "testLongTernary_Num1EqualsNum2"
        val methodName3 = "testLongTernary_Num1NotEqualsNum2"

        val displayName1 = "num1 > num2 : True -> return num1 > num2 ? 1 : num1 == num2 ? 2 : 3"
        val displayName2 = "num1 == num2 : True -> return num1 > num2 ? 1 : num1 == num2 ? 2 : 3"
        val displayName3 = "num1 == num2 : False -> return num1 > num2 ? 1 : num1 == num2 ? 2 : 3"

        val method = Ternary::longTernary
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
    fun testVeryLongTernary() {
        val summary1 = "Test executes conditions:\n" +
                "    (num1 > num2): True\n" +
                "returns from: return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5;\n"
        val summary2 = "Test executes conditions:\n" +
                "    (num1 > num2): False,\n" +
                "    (num1 == num2): False,\n" +
                "    (num2 > num3): False,\n" +
                "    (num2 == num3): False\n" +
                "returns from: return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5;\n"
        val summary3 = "Test executes conditions:\n" +
                "    (num1 > num2): False,\n" +
                "    (num1 == num2): True\n" +
                "returns from: return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5;\n"
        val summary4 = "Test executes conditions:\n" +
                "    (num1 > num2): False,\n" +
                "    (num1 == num2): False,\n" +
                "    (num2 > num3): False,\n" +
                "    (num2 == num3): True\n" +
                "returns from: return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5;\n"
        val summary5 = "Test executes conditions:\n" +
                "    (num1 > num2): False,\n" +
                "    (num1 == num2): False,\n" +
                "    (num2 > num3): True\n" +
                "returns from: return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5;\n"

        val methodName1 = "testVeryLongTernary_Num1GreaterThanNum2"
        val methodName2 = "testVeryLongTernary_Num2NotEqualsNum3"
        val methodName3 = "testVeryLongTernary_Num1EqualsNum2"
        val methodName4 = "testVeryLongTernary_Num2EqualsNum3"
        val methodName5 = "testVeryLongTernary_Num2GreaterThanNum3"

        val displayName1 =
            "num1 > num2 : True -> return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5"
        val displayName2 =
            "num2 == num3 : False -> return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5"
        val displayName3 =
            "num1 == num2 : True -> return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5"
        val displayName4 =
            "num2 == num3 : True -> return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5"
        val displayName5 =
            "num2 > num3 : True -> return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5"

        val method = Ternary::veryLongTernary
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
    fun testMinMax() {
        val summary1 = "Test executes conditions:\n" +
                "    (num1 > num2): False\n" +
                "calls {@link org.utbot.examples.ternary.Ternary#minValue(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        ((a < b)): True\n" +
                "    returns from: return (a < b) ? a : b;\n" +
                "    \n" +
                "Test then returns from: return a;\n"
        val summary2 = "Test executes conditions:\n" +
                "    (num1 > num2): True\n" +
                "calls {@link org.utbot.examples.ternary.Ternary#max(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        (val1 >= val2): True\n" +
                "    returns from: return val1 >= val2 ? val1 : val2;\n" +
                "    \n" +
                "Test further returns from: return a;\n"
        val summary3 = "Test executes conditions:\n" +
                "    (num1 > num2): False\n" +
                "calls {@link org.utbot.examples.ternary.Ternary#minValue(int,int)},\n" +
                "    there it executes conditions:\n" +
                "        ((a < b)): False\n" +
                "    returns from: return (a < b) ? a : b;\n" +
                "    \n" +
                "Test next returns from: return a;\n"

        val methodName1 = "testMinMax_ALessThanB"
        val methodName2 = "testMinMax_Val1GreaterOrEqualVal2"
        val methodName3 = "testMinMax_AGreaterOrEqualB"

        val displayName1 = "a < b : True -> return (a < b) ? a : b"
        val displayName2 = "val1 >= val2 : True -> return val1 >= val2 ? val1 : val2"
        val displayName3 = "a < b : False -> return (a < b) ? a : b"

        val method = Ternary::minMax
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
    fun testIntFunc() {
        val summary1 = "Test executes conditions:\n" +
                "    (num1 > num2): True\n" +
                "invokes:\n" +
                "    org.utbot.examples.ternary.Ternary#intFunc1() once\n" +
                "returns from: return num1 > num2 ? intFunc1() : intFunc2();\n"
        val summary2 = "Test executes conditions:\n" +
                "    (num1 > num2): False\n" +
                "invokes:\n" +
                "    org.utbot.examples.ternary.Ternary#intFunc2() once\n" +
                "returns from: return num1 > num2 ? intFunc1() : intFunc2();\n"

        val methodName1 = "testIntFunc_Num1GreaterThanNum2"
        val methodName2 = "testIntFunc_Num1LessOrEqualNum2"

        val displayName1 = "num1 > num2 : True -> return num1 > num2 ? intFunc1() : intFunc2()"
        val displayName2 = "num1 > num2 : False -> return num1 > num2 ? intFunc1() : intFunc2()"

        val method = Ternary::intFunc
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testTernaryInTheMiddle() {
        val summary1 = "Test executes conditions:\n" +
                "    (num2 > num3): True\n" +
                "returns from: return max(num1 + 228, num2 > num3 ? num2 + 1 : num3 + 2) + 4;\n"
        val summary2 = "Test executes conditions:\n" +
                "    (num2 > num3): False\n" +
                "returns from: return max(num1 + 228, num2 > num3 ? num2 + 1 : num3 + 2) + 4;\n"

        val methodName1 = "testTernaryInTheMiddle_Num2GreaterThanNum3"
        val methodName2 = "testTernaryInTheMiddle_Num2LessOrEqualNum3"

        val displayName1 = "num2 > num3 : True -> return max(num1 + 228, num2 > num3 ? num2 + 1 : num3 + 2) + 4"
        val displayName2 = "num2 > num3 : False -> return max(num1 + 228, num2 > num3 ? num2 + 1 : num3 + 2) + 4"

        val method = Ternary::ternaryInTheMiddle
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

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

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }

    @Test
    fun testTwoIfsOneLine() {
        val summary1 = "Test executes conditions:\n" +
                "    (num1 > num2): False\n" +
                "returns from: return a;\n"
        val summary2 = "Test executes conditions:\n" +
                "    (num1 > num2): True,\n" +
                "    ((num1 - 10) > 0): False\n" +
                "returns from: return a;\n"
        val summary3 = "Test executes conditions:\n" +
                "    (num1 > num2): True,\n" +
                "    ((num1 - 10) > 0): True\n" +
                "returns from: return a;\n"

        val methodName1 = "testTwoIfsOneLine_Num1LessOrEqualNum2"
        val methodName2 = "testTwoIfsOneLine_Num1Minus10LessOrEqualZero"
        val methodName3 = "testTwoIfsOneLine_Num1Minus10GreaterThanZero"

        val displayName1 = "num1 > num2 : False -> return a"
        val displayName2 = "(num1 - 10) > 0 : False -> return a"
        val displayName3 = "(num1 - 10) > 0 : True -> return a"

        val method = Ternary::twoIfsOneLine
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
}