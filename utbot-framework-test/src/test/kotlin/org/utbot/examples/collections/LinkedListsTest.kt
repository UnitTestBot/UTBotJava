package org.utbot.examples.collections

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.isException
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

// TODO failed Kotlin compilation (generics) SAT-1332
internal class LinkedListsTest : UtValueTestCaseChecker(
    testClass = LinkedLists::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {

    @Test
    fun testSet() {
        check(
            LinkedLists::set,
            eq(3),
            { l, _ -> l == null },
            { l, _ -> l.size <= 2 },
            { l, r -> l.size > 2 && r == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testOffer() {
        check(
            LinkedLists::offer,
            eq(3),
            { l, _ -> l == null },
            { l, r -> l != null && l.size <= 1 && r == l },
            { l, r -> l != null && l.size > 1 && r == l + 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testOfferLast() {
        check(
            LinkedLists::offerLast,
            eq(3),
            { l, _ -> l == null },
            { l, r -> l != null && l.size <= 1 && r == l },
            { l, r -> l != null && l.size > 1 && r == l + 1 },
            coverage = DoNotCalculate
        )
    }


    @Test
    fun testAddLast() {
        check(
            LinkedLists::addLast,
            eq(3),
            { l, _ -> l == null },
            { l, r -> l != null && l.size <= 1 && r == l },
            { l, r -> l != null && l.size > 1 && (r == l + 1) },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testPush() {
        check(
            LinkedLists::push,
            eq(3),
            { l, _ -> l == null },
            { l, r -> l != null && l.size <= 1 && r == l },
            { l, r -> l != null && l.size > 1 && r == listOf(1) + l },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testOfferFirst() {
        check(
            LinkedLists::offerFirst,
            eq(3),
            { l, _ -> l == null },
            { l, r -> l != null && l.size <= 1 && r == l },
            { l, r -> l != null && l.size > 1 && r == listOf(1) + l },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testAddFirst() {
        check(
            LinkedLists::addFirst,
            eq(3),
            { l, _ -> l == null },
            { l, r -> l != null && l.size <= 1 && r == l },
            { l, r -> l != null && l.size > 1 && r!!.size == l.size + 1 && r[0] == 1 },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testPeek() {
        checkWithException(
            LinkedLists::peek,
            eq(3),
            { l, _ -> l == null },
            { l, r -> l != null && (l.isEmpty() || l.first() == null) && r.isException<NullPointerException>() },
            { l, r -> l != null && l.isNotEmpty() && r.getOrNull() == l[0] },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testPeekFirst() {
        checkWithException(
            LinkedLists::peekFirst,
            eq(3),
            { l, _ -> l == null },
            { l, r -> l != null && (l.isEmpty() || l.first() == null) && r.isException<NullPointerException>() },
            { l, r -> l != null && l.isNotEmpty() && r.getOrNull() == l[0] },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testPeekLast() {
        checkWithException(
            LinkedLists::peekLast,
            eq(3),
            { l, _ -> l == null },
            { l, r -> l != null && (l.isEmpty() || l.last() == null) && r.isException<NullPointerException>() },
            { l, r -> l != null && l.isNotEmpty() && r.getOrNull() == l.last() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testElement() {
        checkWithException(
            LinkedLists::element,
            eq(4),
            { l, _ -> l == null },
            { l, r -> l != null && l.isEmpty() && r.isException<NoSuchElementException>() },
            { l, r -> l != null && l.isNotEmpty() && l[0] == null && r.isException<NullPointerException>() },
            { l, r -> l != null && l.isNotEmpty() && l[0] != null && r.getOrNull() == l[0] },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetFirst() {
        checkWithException(
            LinkedLists::getFirst,
            eq(4),
            { l, _ -> l == null },
            { l, r -> l != null && l.isEmpty() && r.isException<NoSuchElementException>() },
            { l, _ -> l != null && l.isNotEmpty() && l[0] == null },
            { l, r -> l != null && l.isNotEmpty() && r.getOrNull() == l[0] },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetLast() {
        checkWithException(
            LinkedLists::getLast,
            eq(4),
            { l, _ -> l == null },
            { l, r -> l != null && l.isEmpty() && r.isException<NoSuchElementException>() },
            { l, _ -> l != null && l.isNotEmpty() && l.last() == null },
            { l, r -> l != null && l.isNotEmpty() && r.getOrNull() == l.last() },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testPoll() {
        checkWithException(
            LinkedLists::poll,
            eq(5),
            { l, _ -> l == null },
            { l, r -> l != null && l.isEmpty() && r.isException<NullPointerException>() },
            { l, r -> l != null && l.size == 1 && r.getOrNull() == l },
            { l, _ -> l != null && l.size > 1 && l.first() == null },
            { l, r -> l != null && l.size > 1 && r.getOrNull() == l.subList(1, l.size) },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testPollFirst() {
        checkWithException(
            LinkedLists::pollFirst,
            eq(5),
            { l, _ -> l == null },
            { l, r -> l != null && l.isEmpty() && r.isException<NullPointerException>() },
            { l, r -> l != null && l.size == 1 && r.getOrNull() == l },
            { l, _ -> l != null && l.size > 1 && l.first() == null },
            { l, r -> l != null && l.size > 1 && r.getOrNull() == l.subList(1, l.size) },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testPollLast() {
        checkWithException(
            LinkedLists::pollLast,
            eq(5),
            { l, _ -> l == null },
            { l, r -> l != null && l.isEmpty() && r.isException<NullPointerException>() },
            { l, r -> l != null && l.size == 1 && r.getOrNull() == l },
            { l, _ -> l != null && l.size > 1 && l.last() == null },
            { l, r -> l != null && l.size > 1 && r.getOrNull() == l.subList(0, l.size - 1) },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testRemove() {
        checkWithException(
            LinkedLists::removeFirst,
            eq(5),
            { l, _ -> l == null },
            { l, r -> l != null && l.isEmpty() && r.isException<NoSuchElementException>() },
            { l, r -> l != null && l.size == 1 && r.getOrNull() == l },
            { l, _ -> l != null && l.size > 1 && l.first() == null },
            { l, r -> l != null && l.size > 1 && r.getOrNull() == l.subList(1, l.size) },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testRemoveFirst() {
        checkWithException(
            LinkedLists::removeFirst,
            eq(5),
            { l, _ -> l == null },
            { l, r -> l != null && l.isEmpty() && r.isException<NoSuchElementException>() },
            { l, r -> l != null && l.size == 1 && r.getOrNull() == l },
            { l, _ -> l != null && l.size > 1 && l.first() == null },
            { l, r -> l != null && l.size > 1 && r.getOrNull() == l.subList(1, l.size) },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testRemoveLast() {
        checkWithException(
            LinkedLists::removeLast,
            eq(5),
            { l, _ -> l == null },
            { l, r -> l != null && l.isEmpty() && r.isException<NoSuchElementException>() },
            { l, r -> l != null && l.size == 1 && r.getOrNull() == l },
            { l, _ -> l != null && l.size > 1 && l.last() == null },
            { l, r -> l != null && l.size > 1 && r.getOrNull() == l.subList(0, l.size - 1) },
            coverage = DoNotCalculate
        )
    }

}
