package org.utbot.examples.structures

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.keyContain
import org.utbot.tests.infrastructure.keyMatch
import org.utbot.framework.plugin.api.DocCodeStmt
import org.utbot.framework.plugin.api.DocPreTagStatement
import org.utbot.framework.plugin.api.DocRegularStmt
import org.utbot.framework.plugin.api.DocStatement
import java.util.LinkedList
import java.util.TreeMap
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

internal class StandardStructuresTest : UtValueTestCaseChecker(
    testClass = StandardStructures::class,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    @Disabled("TODO down cast for object wrapper JIRA:1480")
    fun testGetList() {
        check(
            StandardStructures::getList,
            eq(4),
            { l, r -> l is ArrayList && r is ArrayList },
            { l, r -> l is LinkedList && r is LinkedList },
            { l, r -> l == null && r == null },
            { l, r ->
                l !is ArrayList && l !is LinkedList && l != null && r !is ArrayList && r !is LinkedList && r != null
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("TODO down cast for object wrapper JIRA:1480")
    fun testGetMap() {
        check(
            StandardStructures::getMap,
            eq(3),
            { m, r -> m is TreeMap && r is TreeMap },
            { m, r -> m == null && r == null },
            { m, r -> m !is TreeMap && m != null && r !is TreeMap && r != null },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testGetDeque() {
        val dequeSummary = listOf<DocStatement>(
            DocPreTagStatement(
                listOf(
                    DocRegularStmt("Test "),
                    DocRegularStmt("executes conditions:\n"),
                    DocRegularStmt("    "),
                    DocCodeStmt("(deque instanceof LinkedList): False"),
                    DocRegularStmt(",\n"),
                    DocRegularStmt("    "),
                    DocCodeStmt("(deque == null): True"),
                    DocRegularStmt("\n"),
                    DocRegularStmt("returns from: "),
                    DocCodeStmt("return null;"),
                    DocRegularStmt("\n")
                )
            )
        )

        check(
            StandardStructures::getDeque,
            eq(4),
            { d, r -> d is java.util.ArrayDeque && r is java.util.ArrayDeque },
            { d, r -> d is LinkedList && r is LinkedList },
            { d, r -> d == null && r == null },
            { d, r ->
                d !is java.util.ArrayDeque<*> && d !is LinkedList && d != null && r !is java.util.ArrayDeque<*> && r !is LinkedList && r != null
            },
            coverage = DoNotCalculate,
            summaryTextChecks = listOf(
                keyContain(DocCodeStmt("(deque instanceof ArrayDeque): True")),
                keyContain(DocCodeStmt("(deque instanceof LinkedList): True")),
                keyContain(DocCodeStmt("(deque == null): True")),
                keyMatch(dequeSummary)
            ),
            summaryNameChecks = listOf(
                keyMatch("testGetDeque_DequeInstanceOfArrayDeque"),
                keyMatch("testGetDeque_DequeInstanceOfLinkedList"),
                keyMatch("testGetDeque_DequeEqualsNull"),
                keyMatch("testGetDeque_DequeNotEqualsNull"),
            ),
            summaryDisplayNameChecks = listOf(
                keyContain("deque", "instance", "ArrayDeque"),
                keyContain("deque", "instance", "LinkedList"),
                keyContain("deque == null", "True"),
                keyContain("deque == null", "False"),
            )
        )
    }
}