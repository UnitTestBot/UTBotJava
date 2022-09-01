package org.utbot.examples.collections

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration
import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.isException

class QueueUsagesTest : UtValueTestCaseChecker(
    testClass = QueueUsages::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
    CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
    CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testCreateArrayDeque() {
        checkWithException(
            QueueUsages::createArrayDeque,
            eq(3),
            { init, next, r -> init == null && next == null && r.isException<NullPointerException>() },
            { init, next, r -> init != null && next == null && r.isException<NullPointerException>() },
            { init, next, r -> init != null && next != null && r.getOrNull() == 2 },
        )
    }

    @Test
    fun testCreateLinkedList() {
        checkWithException(
            QueueUsages::createLinkedList,
            eq(1),
            { _, _, r -> r.getOrNull()!! == 2 },
        )
    }

    @Test
    fun testCreateLinkedBlockingDeque() {
        checkWithException(
            QueueUsages::createLinkedBlockingDeque,
            eq(3),
            { init, next, r -> init == null && next == null && r.isException<NullPointerException>()  },
            { init, next, r -> init != null && next == null && r.isException<NullPointerException>() },
            { init, next, r -> init != null && next != null && r.getOrNull() == 2 },
        )
    }

    @Test
    fun testContainsQueue() {
        checkWithException(
            QueueUsages::containsQueue,
            eq(3),
            { q, _, r -> q == null && r.isException<NullPointerException>() },
            { q, x, r -> x in q && r.getOrNull() == 1 },
            { q, x, r -> x !in q && r.getOrNull() == 0 },
        )
    }

    @Test
    fun testAddQueue() {
        checkWithException(
            QueueUsages::addQueue,
            eq(3),
            { q, _, r -> q == null && r.isException<NullPointerException>() },
            { q, x, r -> q != null && x in r.getOrNull()!! },
            { q, x, r -> q != null && x == null && r.isException<NullPointerException>() },        )
    }

    @Test
    fun testAddAllQueue() {
        checkWithException(
            QueueUsages::addAllQueue,
            eq(3),
            { q, _, r -> q == null && r.isException<NullPointerException>() },
            { q, x, r -> q != null && x in r.getOrNull()!! }, // we can cover this line with x == null or x != null
            { q, x, r -> q != null && x == null && r.isException<NullPointerException>() },
        )
    }

    @Test
    fun testCastQueueToDeque() {
        check(
            QueueUsages::castQueueToDeque,
            eq(2),
            { q, r -> q !is java.util.Deque<*> && r == null },
            { q, r -> q is java.util.Deque<*> && r is java.util.Deque<*> },
        )
    }

    @Test
    fun testCheckSubtypesOfQueue() {
        check(
            QueueUsages::checkSubtypesOfQueue,
            eq(4),
            { q, r -> q == null && r == 0 },
            { q, r -> q is java.util.LinkedList<*> && r == 1 },
            { q, r -> q is java.util.ArrayDeque<*> && r == 2 },
            { q, r -> q !is java.util.LinkedList<*> && q !is java.util.ArrayDeque && r == 3 }
        )
    }

    @Test
    @Disabled("TODO: Related to https://github.com/UnitTestBot/UTBotJava/issues/820")
    fun testCheckSubtypesOfQueueWithUsage() {
        check(
            QueueUsages::checkSubtypesOfQueueWithUsage,
            eq(4),
            { q, r -> q == null && r == 0 },
            { q, r -> q is java.util.LinkedList<*> && r == 1 },
            { q, r -> q is java.util.ArrayDeque<*> && r == 2 },
            { q, r -> q !is java.util.LinkedList<*> && q !is java.util.ArrayDeque && r == 3 } // this is uncovered
        )
    }

    @Test
    fun testAddConcurrentLinkedQueue() {
        checkWithException(
            QueueUsages::addConcurrentLinkedQueue,
            eq(3),
            { q, _, r -> q == null && r.isException<NullPointerException>() },
            { q, x, r -> q != null && x != null && x in r.getOrNull()!! },
            { q, x, r -> q != null && x == null && r.isException<NullPointerException>() },
        )
    }
}