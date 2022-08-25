package org.utbot.examples.algorithms

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.examples.algorithms.CorrectBracketSequences.isBracket
import org.utbot.examples.algorithms.CorrectBracketSequences.isOpen
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException
import org.utbot.tests.infrastructure.keyMatch
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.DocCodeStmt
import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

internal class CorrectBracketSequencesTest : UtValueTestCaseChecker(
    testClass = CorrectBracketSequences::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration) // TODO generics in lists
    )
) {
    @Test
    fun testIsOpen() {
        val isOpenSummary = listOf(
            DocPreTagStatement(
                listOf(
                    DocRegularStmt("Test "),
                    DocRegularStmt("returns from: "),
                    DocCodeStmt("return a == '(' || a == '{' || a == '[';"),
                    DocRegularStmt("\n")
                )
            )
        )

        checkStaticMethod(
            CorrectBracketSequences::isOpen,
            eq(4),
            { c, r -> c == '(' && r == true },
            { c, r -> c == '{' && r == true },
            { c, r -> c == '[' && r == true },
            { c, r -> c !in "({[".toList() && r == false },
            summaryNameChecks = listOf(
                keyMatch("testIsOpen_AEqualsCharOrAEqualsCharOrAEqualsChar"),
                keyMatch("testIsOpen_ANotEqualsCharOrANotEqualsCharOrANotEqualsChar")
            ),
            summaryDisplayNameChecks = listOf(
                keyMatch("return a == '(' || a == '{' || a == '[' : False -> return a == '(' || a == '{' || a == '['"),
                keyMatch("return a == '(' || a == '{' || a == '[' : True -> return a == '(' || a == '{' || a == '['")
            ),
            summaryTextChecks = listOf(
                keyMatch(isOpenSummary)
            )
        )
    }

    @Test
    fun testIsBracket() {
        val isBracketSummary = listOf(
            DocPreTagStatement(
                listOf(
                    DocRegularStmt("Test "),
                    DocRegularStmt("returns from: "),
                    DocCodeStmt("return isOpen(a) || a == ')' || a == '}' || a == ']';"),
                    DocRegularStmt("\n")
                )
            )
        )
        checkStaticMethod(
            CorrectBracketSequences::isBracket,
            eq(7),
            { c, r -> c == '(' && r == true },
            { c, r -> c == '{' && r == true },
            { c, r -> c == '[' && r == true },
            { c, r -> c == ')' && r == true },
            { c, r -> c == '}' && r == true },
            { c, r -> c == ']' && r == true },
            { c, r -> c !in "(){}[]".toList() && r == false },
            summaryNameChecks = listOf(
                keyMatch("testIsBracket_IsOpenOrANotEqualsCharOrANotEqualsCharOrANotEqualsChar"),
                keyMatch("testIsBracket_IsOpenOrAEqualsCharOrAEqualsCharOrAEqualsChar")
            ),
            summaryDisplayNameChecks = listOf(
                keyMatch("return isOpen(a) || a == ')' || a == '}' || a == ']' : False -> return isOpen(a) || a == ')' || a == '}' || a == ']'"),
                keyMatch("return isOpen(a) || a == ')' || a == '}' || a == ']' : True -> return isOpen(a) || a == ')' || a == '}' || a == ']'")
            ),
            summaryTextChecks = listOf(
                keyMatch(isBracketSummary)
            )
        )
    }

    @Test
    fun testIsTheSameType() {
        val isTheSameTypeSummary = listOf(
            DocPreTagStatement(
                listOf(
                    DocRegularStmt("Test "),
                    DocRegularStmt("returns from: "),
                    DocCodeStmt("return a == '(' && b == ')' || a == '{' && b == '}' || a == '[' && b == ']';"),
                    DocRegularStmt("\n")
                )
            )
        )
        checkStaticMethod(
            CorrectBracketSequences::isTheSameType,
            ignoreExecutionsNumber,
            { a, b, r -> a == '(' && b == ')' && r == true },
            { a, b, r -> a == '{' && b == '}' && r == true },
            { a, b, r -> a == '[' && b == ']' && r == true },
            { a, b, r -> a == '(' && b != ')' && r == false },
            { a, b, r -> a == '{' && b != '}' && r == false },
            { a, b, r -> a == '[' && b != ']' && r == false },
            { a, b, r -> (a != '(' || b != ')') && (a != '{' || b != '}') && (a != '[' || b != ']') && r == false },
            summaryNameChecks = listOf(
                keyMatch("testIsTheSameType_ANotEqualsCharAndBNotEqualsCharOrANotEqualsCharAndBNotEqualsCharOrANotEqualsCharAndBNotEqualsChar"),
                keyMatch("testIsTheSameType_AEqualsCharAndBEqualsCharOrAEqualsCharAndBEqualsCharOrAEqualsCharAndBEqualsChar"),
            ),
            summaryDisplayNameChecks = listOf(
                keyMatch("return a == '(' && b == ')' || a == '{' && b == '}' || a == '[' && b == ']' : False -> return a == '(' && b == ')' || a == '{' && b == '}' || a == '[' && b == ']'"),
                keyMatch("return a == '(' && b == ')' || a == '{' && b == '}' || a == '[' && b == ']' : True -> return a == '(' && b == ')' || a == '{' && b == '}' || a == '[' && b == ']'")
            ),
            summaryTextChecks = listOf(
                keyMatch(isTheSameTypeSummary)
            )
        )
    }

    @Test
    fun testIsCbs() {
        val method = CorrectBracketSequences::isCbs
        checkStaticMethodWithException(
            method,
            ignoreExecutionsNumber,
            { chars, r -> chars == null && r.isException<NullPointerException>() },
            { chars, r -> chars != null && chars.isEmpty() && r.getOrNull() == true },
            { chars, r -> chars.any { it == null } && r.isException<NullPointerException>() },
            { chars, r -> !isBracket(chars.first()) && r.getOrNull() == false },
            { chars, r -> !isOpen(chars.first()) && r.getOrNull() == false },
            { chars, _ -> isOpen(chars.first()) },
            { chars, r -> chars.all { isOpen(it) } && r.getOrNull() == false },
            { chars, _ ->
                val charsWithoutFirstOpenBrackets = chars.dropWhile { isOpen(it) }
                val firstNotOpenBracketChar = charsWithoutFirstOpenBrackets.first()

                isBracket(firstNotOpenBracketChar) && !isOpen(firstNotOpenBracketChar)
            },
        )
    }
}