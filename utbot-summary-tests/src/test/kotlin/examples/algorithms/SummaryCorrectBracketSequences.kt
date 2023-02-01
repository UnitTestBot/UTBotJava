package examples.algorithms

import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.utbot.examples.algorithms.CorrectBracketSequences
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate

class SummaryCorrectBracketSequences : SummaryTestCaseGeneratorTest(
    CorrectBracketSequences::class,
) {
    @Test
    fun testIsTheSameType() {
        val commonSummary = "Test returns from: return a == '(' && b == ')' || a == '{' && b == '}' || a == '[' && b == ']';\n"

        val methodName1 = "testIsTheSameType_ANotEqualsCharAndBNotEqualsCharOrANotEqualsCharAndBNotEqualsCharOrANotEqualsCharAndBNotEqualsChar"
        val methodName2 = "testIsTheSameType_ANotEqualsCharAndBNotEqualsCharOrANotEqualsCharAndBNotEqualsCharOrANotEqualsCharAndBNotEqualsChar_1"
        val methodName3 = "testIsTheSameType_AEqualsCharAndBEqualsCharOrAEqualsCharAndBEqualsCharOrAEqualsCharAndBEqualsChar"
        val methodName4 = "testIsTheSameType_ANotEqualsCharAndBNotEqualsCharOrANotEqualsCharAndBNotEqualsCharOrANotEqualsCharAndBNotEqualsChar_2"
        val methodName5 = "testIsTheSameType_ANotEqualsCharAndBNotEqualsCharOrANotEqualsCharAndBNotEqualsCharOrANotEqualsCharAndBNotEqualsChar_3"
        val methodName6 = "testIsTheSameType_AEqualsCharAndBEqualsCharOrAEqualsCharAndBEqualsCharOrAEqualsCharAndBEqualsChar_1"
        val methodName7 = "testIsTheSameType_AEqualsCharAndBEqualsCharOrAEqualsCharAndBEqualsCharOrAEqualsCharAndBEqualsChar_2"

        val commonDiplayName1 = "return a == '(' && b == ')' || a == '{' && b == '}' || a == '[' && b == ']' : False -> return a == '(' && b == ')' || a == '{' && b == '}' || a == '[' && b == ']'"
        val commonDisplayName2 = "return a == '(' && b == ')' || a == '{' && b == '}' || a == '[' && b == ']' : True -> return a == '(' && b == ')' || a == '{' && b == '}' || a == '[' && b == ']'"

        val summaryKeys = listOf(
            commonSummary
        )

        val displayNames = listOf(
            commonDiplayName1,
            commonDisplayName2
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

        val method = CorrectBracketSequences::isTheSameType
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}