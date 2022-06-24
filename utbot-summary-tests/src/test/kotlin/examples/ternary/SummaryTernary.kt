package examples.ternary

import examples.SummaryTestCaseGeneratorTest
import org.junit.Ignore
import org.junit.jupiter.api.Disabled
import org.utbot.examples.ternary.Ternary
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Disabled
class SummaryTernary : SummaryTestCaseGeneratorTest(
    Ternary::class,
) {

    val summaryMax1 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (val1 >= val2): False}\n" +
            "returns from: {@code return val1 >= val2 ? val1 : val2;}\n" +
            "</pre>"
    val summaryMax2 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (val1 >= val2): True}\n" +
            "returns from: {@code return val1 >= val2 ? val1 : val2;}\n" +
            "</pre>"

    @Test
    fun testMax() {
        checkTwoArguments(
            Ternary::max,
            summaryKeys = listOf(
                summaryMax1,
                summaryMax2
            ),
            displayNames = listOf(
                "val1 >= val2 : False -> return val1 >= val2 ? val1 : val2",
                "val1 >= val2 : True -> return val1 >= val2 ? val1 : val2"
            )
        )
    }

    val summarySimpleOperation = "<pre>\n" +
            "Test returns from: {@code return result;}\n" +
            "</pre>"

    @Test
    fun testSimpleOperation() {
        checkTwoArguments(
            Ternary::simpleOperation,
            summaryKeys = listOf(summarySimpleOperation),
            displayNames = listOf(
                "-> return result"
            )
        )
    }

    val summaryStringExpr1 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num > 10): True}\n" +
            "returns from: {@code return num > 10 ? \"Number is greater than 10\" : num > 5 ? \"Number is greater than 5\" : \"Number is less than equal to 5\";}\n" +
            "</pre>"
    val summaryStringExpr2 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num > 10): False},\n" +
            "    {@code (num > 5): False}\n" +
            "returns from: {@code return num > 10 ? \"Number is greater than 10\" : num > 5 ? \"Number is greater than 5\" : \"Number is less than equal to 5\";}\n" +
            "</pre>"
    val summaryStringExpr3 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num > 10): False},\n" +
            "    {@code (num > 5): True}\n" +
            "returns from: {@code return num > 10 ? \"Number is greater than 10\" : num > 5 ? \"Number is greater than 5\" : \"Number is less than equal to 5\";}\n" +
            "</pre>"

    @Test
    fun testStringExpr() {
        checkOneArgument(
            Ternary::stringExpr,
            summaryKeys = listOf(
                summaryStringExpr1,
                summaryStringExpr2,
                summaryStringExpr3
            )
        )
    }

    @Test
    @Tag("slow")
    fun testParse() {
        checkOneArgument(
            Ternary::parse,
            summaryKeys = listOf()
        )
    }

    val summaryMinValue1 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code ((a < b)): False}\n" +
            "returns from: {@code return (a < b) ? a : b;}\n" +
            "</pre>"
    val summaryMinValue2 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code ((a < b)): True}\n" +
            "returns from: {@code return (a < b) ? a : b;}\n" +
            "</pre>"

    @Test
    fun testMinValue() {
        checkTwoArguments(
            Ternary::minValue,
            summaryKeys = listOf(
                summaryMinValue1,
                summaryMinValue2
            ),
            displayNames = listOf(
                "a < b : False -> return (a < b) ? a : b",
                "a < b : True -> return (a < b) ? a : b"
            )
        )
    }

    val summarySubDelay1 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (flag): False}\n" +
            "returns from: {@code return flag ? 100 : 0;}\n" +
            "</pre>"
    val summarySubDelay2 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (flag): True}\n" +
            "returns from: {@code return flag ? 100 : 0;}\n" +
            "</pre>"

    @Test
    fun testSubDelay() {
        checkOneArgument(
            Ternary::subDelay,
            summaryKeys = listOf(
                summarySubDelay1,
                summarySubDelay2
            ),
            displayNames = listOf(
                "flag : False -> return flag ? 100 : 0",
                "flag : True -> return flag ? 100 : 0"
            )
        )
    }

    val summaryPlusOrMinus1 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code ((num1 > num2)): False}\n" +
            "returns from: {@code return (num1 > num2) ? (num1 + num2) : (num1 - num2);}\n" +
            "</pre>"
    val summaryPlusOrMinus2 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code ((num1 > num2)): True}\n" +
            "returns from: {@code return (num1 > num2) ? (num1 + num2) : (num1 - num2);}\n" +
            "</pre>"

    @Test
    fun testPlusOrMinus() {
        checkTwoArguments(
            Ternary::plusOrMinus,
            summaryKeys = listOf(
                summaryPlusOrMinus1,
                summaryPlusOrMinus2
            )
        )
    }

    val summaryLongTernary1 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): True}\n" +
            "returns from: {@code return num1 > num2 ? 1 : num1 == num2 ? 2 : 3;}\n" +
            "</pre>"
    val summaryLongTernary2 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): False},\n" +
            "    {@code (num1 == num2): True}\n" +
            "returns from: {@code return num1 > num2 ? 1 : num1 == num2 ? 2 : 3;}\n" +
            "</pre>"
    val summaryLongTernary3 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): False},\n" +
            "    {@code (num1 == num2): False}\n" +
            "returns from: {@code return num1 > num2 ? 1 : num1 == num2 ? 2 : 3;}\n" +
            "</pre>"

    @Test
    fun testLongTernary() {
        checkTwoArguments(
            Ternary::longTernary,
            summaryKeys = listOf(
                summaryLongTernary1,
                summaryLongTernary2,
                summaryLongTernary3
            )
        )
    }

    val summaryVeryLongTernary1 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): True}\n" +
            "returns from: {@code return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5;}\n" +
            "</pre>"
    val summaryVeryLongTernary2 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): False},\n" +
            "    {@code (num1 == num2): False},\n" +
            "    {@code (num2 > num3): False},\n" +
            "    {@code (num2 == num3): False}\n" +
            "returns from: {@code return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5;}\n" +
            "</pre>"
    val summaryVeryLongTernary3 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): False},\n" +
            "    {@code (num1 == num2): True}\n" +
            "returns from: {@code return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5;}\n" +
            "</pre>"
    val summaryVeryLongTernary4 = "Test executes conditions:\n" +
            "    {@code (num1 > num2): False},\n" +
            "    {@code (num1 == num2): False},\n" +
            "    {@code (num2 > num3): False},\n" +
            "    {@code (num2 == num3): True}\n" +
            "returns from: {@code return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5;}\n" +
            "</pre>"
    val summaryVeryLongTernary5 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): False},\n" +
            "    {@code (num1 == num2): False},\n" +
            "    {@code (num2 > num3): True}\n" +
            "returns from: {@code return num1 > num2 ? 1 : num1 == num2 ? 2 : num2 > num3 ? 3 : num2 == num3 ? 4 : 5;}\n" +
            "</pre>"

    @Test
    fun testVeryLongTernary() {
        checkThreeArguments(
            Ternary::veryLongTernary,
            summaryKeys = listOf(
                summaryVeryLongTernary1,
                summaryVeryLongTernary2,
                summaryVeryLongTernary3,
                summaryVeryLongTernary4,
                summaryVeryLongTernary5
            )
        )
    }

    val summaryMinMax1 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): False}\n" +
            "calls Ternary::minValue,\n" +
            "    there it executes conditions:\n" +
            "        {@code ((a < b)): True}\n" +
            "    returns from: {@code return (a < b) ? a : b;}"
    val summaryMinMax2 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): True}\n" +
            "calls Ternary::max,\n" +
            "    there it executes conditions:\n" +
            "        {@code (val1 >= val2): True}\n" +
            "    returns from: {@code return val1 >= val2 ? val1 : val2;}"
    val summaryMinMax3 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): False}\n" +
            "calls Ternary::minValue,\n" +
            "    there it executes conditions:\n" +
            "        {@code ((a < b)): False}\n" +
            "    returns from: {@code return (a < b) ? a : b;}"

    @Test
    fun testMinMax() {
        checkTwoArguments(
            Ternary::minMax,
            summaryKeys = listOf(
                summaryMinMax1,
                summaryMinMax2,
                summaryMinMax3
            )
        )
    }

    val summaryIncFunc1 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): False}\n" +
            "invokes:\n" +
            "    Ternary::intFunc2 once\n" +
            "returns from: {@code return num1 > num2 ? intFunc1() : intFunc2();}\n" +
            "</pre>"
    val summaryIncFunc2 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): True}\n" +
            "invokes:\n" +
            "    Ternary::intFunc1 once\n" +
            "returns from: {@code return num1 > num2 ? intFunc1() : intFunc2();}\n" +
            "</pre>"

    @Test
    fun testIntFunc() {
        checkTwoArguments(
            Ternary::intFunc,
            summaryKeys = listOf(
                summaryIncFunc1,
                summaryIncFunc2
            ),
            displayNames = listOf(
                "num1 > num2 : False -> return num1 > num2 ? intFunc1() : intFunc2()",
                "num1 > num2 : True -> return num1 > num2 ? intFunc1() : intFunc2()"
            )
        )
    }

    val summaryTernaryInTheMiddle1 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num2 > num3): True}\n" +
            "calls Ternary::max,\n" +
            "    there it executes conditions:\n" +
            "        {@code (val1 >= val2): False}\n" +
            "    returns from: {@code return val1 >= val2 ? val1 : val2;}"
    val summaryTernaryInTheMiddle2 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num2 > num3): False}\n" +
            "calls Ternary::max,\n" +
            "    there it executes conditions:\n" +
            "        {@code (val1 >= val2): False}\n" +
            "    returns from: {@code return val1 >= val2 ? val1 : val2;}"
    val summaryTernaryInTheMiddle3 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num2 > num3): False}\n" +
            "calls Ternary::max,\n" +
            "    there it executes conditions:\n" +
            "        {@code (val1 >= val2): True}\n" +
            "    returns from: {@code return val1 >= val2 ? val1 : val2;}"

    @Test
    fun testTernaryInTheMiddle() {
        checkThreeArguments(
            Ternary::ternaryInTheMiddle,
            summaryKeys = listOf(
                summaryTernaryInTheMiddle1,
                summaryTernaryInTheMiddle2,
                summaryTernaryInTheMiddle3
            ),
            displayNames = listOf(
                "val1 >= val2 : False -> return val1 >= val2 ? val1 : val2",
                "val2 : False -> return val1 >= val2 ? val1 : val2",
                "val1 >= val2 : True -> return val1 >= val2 ? val1 : val2"
            )
        )
    }

    val summaryTwoIfsOneLine1 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): False}\n" +
            "returns from: {@code return a;}\n" +
            "</pre>"
    val summaryTwoIfsOneLine2 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): True},\n" +
            "    {@code ((num1 - 10) > 0): False}\n" +
            "returns from: {@code return a;}\n" +
            "</pre>"
    val summaryTwoIfsOneLine3 = "<pre>\n" +
            "Test executes conditions:\n" +
            "    {@code (num1 > num2): True},\n" +
            "    {@code ((num1 - 10) > 0): True}\n" +
            "returns from: {@code return a;}\n" +
            "</pre>"

    @Test
    fun testTwoIfsOneLine() {
        checkTwoArguments(
            Ternary::twoIfsOneLine,
            summaryKeys = listOf(
                summaryTwoIfsOneLine1,
                summaryTwoIfsOneLine2,
                summaryTwoIfsOneLine3
            ),
            displayNames = listOf(
                "num1 > num2 : False -> return a",
                "(num1 - 10) > 0 : False -> return a",
                "(num1 - 10) > 0 : True -> return a"
            )
        )
    }
}