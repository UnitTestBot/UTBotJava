package org.utbot.examples.algorithms

import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.examples.algorithms.CorrectBracketSequences.isBracket
import org.utbot.examples.algorithms.CorrectBracketSequences.isOpen
import org.utbot.testcheckers.eq
import org.utbot.testing.CodeGeneration
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.ignoreExecutionsNumber
import org.utbot.testing.isException

internal class CorrectBracketSequencesTest : UtValueTestCaseChecker(
    testClass = CorrectBracketSequences::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration) // TODO generics in lists
    )
) {
    @Test
    fun testIsOpen() {
        checkStaticMethod(
            CorrectBracketSequences::isOpen,
            eq(4),
            { c, r -> c == '(' && r == true },
            { c, r -> c == '{' && r == true },
            { c, r -> c == '[' && r == true },
            { c, r -> c !in "({[".toList() && r == false }
        )
    }

    @Test
    fun testIsBracket() {
        checkStaticMethod(
            CorrectBracketSequences::isBracket,
            eq(7),
            { c, r -> c == '(' && r == true },
            { c, r -> c == '{' && r == true },
            { c, r -> c == '[' && r == true },
            { c, r -> c == ')' && r == true },
            { c, r -> c == '}' && r == true },
            { c, r -> c == ']' && r == true },
            { c, r -> c !in "(){}[]".toList() && r == false }
        )
    }

    @Test
    fun testIsTheSameType() {
        checkStaticMethod(
            CorrectBracketSequences::isTheSameType,
            ignoreExecutionsNumber,
            { a, b, r -> a == '(' && b == ')' && r == true },
            { a, b, r -> a == '{' && b == '}' && r == true },
            { a, b, r -> a == '[' && b == ']' && r == true },
            { a, b, r -> a == '(' && b != ')' && r == false },
            { a, b, r -> a == '{' && b != '}' && r == false },
            { a, b, r -> a == '[' && b != ']' && r == false },
            { a, b, r -> (a != '(' || b != ')') && (a != '{' || b != '}') && (a != '[' || b != ']') && r == false }
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