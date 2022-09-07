package org.utbot.examples.collections

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.AtLeast
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.between
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.ge
import org.utbot.testcheckers.withPushingStateFromPathSelectorForConcrete
import org.utbot.testcheckers.withoutMinimization
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation SAT-1332
internal class SetsTest : UtValueTestCaseChecker(
    testClass = Sets::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun createTest() {
        check(
            Sets::create,
            eq(3),
            { a, _ -> a == null },
            { a, r -> a != null && a.isEmpty() && r!!.isEmpty() },
            { a, r -> a != null && a.isNotEmpty() && r != null && r.isNotEmpty() && r.containsAll(a.toList()) },
        )
    }

    @Test
    fun testSetContainsInteger() {
        check(
            Sets::setContainsInteger,
            ignoreExecutionsNumber,
            { set, _, _, _ -> set == null },
            { set, a, _, r -> 1 + a in set && r != null && 1 + a !in r && set.remove(1 + a) && r == set },
            { set, a, _, r -> 1 + a !in set && set.isEmpty() && r == null },
            { set, a, b, r -> 1 + a !in set && set.isNotEmpty() && r != null && r == set && 4 + a + b !in r },
        )
    }

    @Test
    @Disabled("Does not find positive branches JIRA:1529")
    fun testSetContains() {
        check(
            Sets::setContains,
            eq(-1),
        )
    }

    @Test
    fun testSimpleContains() {
        check(
            Sets::simpleContains,
            ignoreExecutionsNumber,
            { set, _ -> set == null },
            { set, r -> set != null && "aaa" in set && r == true },
            { set, r -> set != null && "aaa" !in set && r == false }
        )
    }

    @Test
    @Disabled("Same problem from testSetContains JIRA:1529")
    fun testMoreComplicatedContains() {
        check(
            Sets::moreComplicatedContains,
            eq(-1), // TODO how many branches do we have?
        )
    }


    @Test
    fun testFindAllChars() {
        check(
            Sets::findAllChars,
            eq(3),
            { s, _ -> s == null },
            { s, result -> s == "" && result!!.isEmpty() },
            { s, result -> s != "" && result == s.toCollection(mutableSetOf()) },
        )
    }

    @Test
    fun testRemoveSpace() {
        val resultFun = { set: Set<Char> -> listOf(' ', '\t', '\r', '\n').intersect(set).size }
        check(
            Sets::removeSpace,
            ge(3),
            { set, _ -> set == null },
            { set, res -> ' ' in set && resultFun(set) == res },
            { set, res -> '\t' in set && resultFun(set) == res },
            { set, res -> '\n' in set && resultFun(set) == res },
            { set, res -> '\r' in set && resultFun(set) == res },
            { set, res -> ' ' !in set && resultFun(set) == res },
            { set, res -> '\t' !in set && resultFun(set) == res },
            { set, res -> '\n' !in set && resultFun(set) == res },
            { set, res -> '\r' !in set && resultFun(set) == res },
        )
    }

    @Test
    fun addElementsTest() {
        check(
            Sets::addElements,
            ge(5),
            { set, _, _ -> set == null },
            { set, a, _ -> set != null && set.isNotEmpty() && a == null },
            { set, _, r -> set.isEmpty() && r == set },
            { set, a, r -> set.isNotEmpty() && a.isEmpty() && r == set },
            { set, a, r ->
                set.size >= 1 && a.isNotEmpty() && r == set.toMutableSet().apply { addAll(a.toTypedArray()) }
            },
        )
    }

    @Test
    fun removeElementsTest() {
        check(
            Sets::removeElements,
            between(6..8),
            { set, _, _, _ -> set == null },
            { set, i, j, res -> set != null && i !in set && j !in set && res == -1 },
            { set, i, j, res -> set != null && set.size >= 1 && i !in set && j in set && res == 4 },
            { set, i, j, res -> set != null && set.size >= 1 && i in set && (j !in set || j == i) && res == 3 },
            { set, i, j, res -> set != null && set.size >= 2 && i in set && j in set && i > j && res == 2 },
            { set, i, j, res -> set != null && set.size >= 2 && i in set && j in set && i < j && res == 1 },
            coverage = AtLeast(94) // unreachable branch
        )
    }

    @Test
    fun createWithDifferentTypeTest() {
        check(
            Sets::createWithDifferentType,
            eq(2),
            { seed, r -> seed % 2 != 0 && r is java.util.LinkedHashSet },
            { seed, r -> seed % 2 == 0 && r !is java.util.LinkedHashSet && r is java.util.HashSet },
        )
    }

    @Test
    fun removeCustomObjectTest() {
        withoutMinimization { // TODO: JIRA:1506
            check(
                Sets::removeCustomObject,
                ge(4),
                { set, _, _ -> set == null },
                { set, _, result -> set.isEmpty() && result == 0 },
                { set, i, result -> set.isNotEmpty() && CustomClass(i) !in set && result == 0 },
                { set, i, result -> set.isNotEmpty() && CustomClass(i) in set && result == 1 },
            )
        }
    }

    @Test
    fun testAddAllElements() {
        withPushingStateFromPathSelectorForConcrete {
            check(
                Sets::addAllElements,
                ignoreExecutionsNumber,
                { set, _, _ -> set == null },
                { set, other, _ -> set != null && other == null },
                { set, other, result -> set.containsAll(other) && result == 0 },
                { set, other, result -> !set.containsAll(other) && result == 1 },
                // TODO: Cannot find branch with result == 2
                { set, other, result -> !set.containsAll(other) && other.any { it in set } && result == 2 },
            )
        }
    }

    @Test
    fun testRemoveAllElements() {
        withPushingStateFromPathSelectorForConcrete {
            check(
                Sets::removeAllElements,
                ignoreExecutionsNumber,
                { set, _, _ -> set == null },
                { set, other, _ -> set != null && other == null },
                { set, other, result -> other.all { it !in set } && result == 0 },
                { set, other, result -> set.containsAll(other) && result == 1 },
                //TODO: JIRA:1666 -- Engine ignores branches in Wrappers sometimes
                // TODO: cannot find branch with result == 2
                // { set, other, result -> !set.containsAll(other) && other.any { it in set } && result == 2 },
                coverage = DoNotCalculate
            )
        }
    }

    @Test
    fun testRetainAllElements() {
        check(
            Sets::retainAllElements,
            ge(4),
            { set, _, _ -> set == null },
            { set, other, _ -> set != null && other == null },
            { set, other, result -> other.containsAll(set) && result == 1 },
            { set, other, result -> set.any { it !in other } && result == 0 },
        )
    }

    @Test
    fun testContainsAllElements() {
        check(
            Sets::containsAllElements,
            ge(5),
            { set, _, _ -> set == null },
            { set, other, _ -> set != null && other == null },
            { set, other, result -> set.isEmpty() || other.isEmpty() && result == -1 },
            { set, other, result -> set.isNotEmpty() && other.isNotEmpty() && set.containsAll(other) && result == 1 },
            { set, other, result -> set.isNotEmpty() && other.isNotEmpty() && !set.containsAll(other) && result == 0 },
        )
    }


    @Test
    fun testClearElements() {
        check(
            Sets::clearElements,
            eq(3),
            { set, _ -> set == null },
            { set, result -> set.isEmpty() && result == 0 },
            { set, result -> set.isNotEmpty() && result == 1 },
            coverage = AtLeast(85) // unreachable final return
        )
    }


    @Test
    fun testContainsElement() {
        check(
            Sets::containsElement,
            between(3..5),
            { set, _, _ -> set == null },
            { set, i, result -> i !in set && result == 0 },
            { set, i, result -> i in set && result == 1 },
        )
    }
}