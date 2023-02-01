package org.utbot.examples.structures

import java.util.LinkedList
import java.util.TreeMap
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.testcheckers.eq
import org.utbot.testing.CodeGeneration
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

internal class StandardStructuresTest : UtValueTestCaseChecker(
    testClass = StandardStructures::class,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
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
        check(
            StandardStructures::getDeque,
            eq(4),
            { d, r -> d is java.util.ArrayDeque && r is java.util.ArrayDeque },
            { d, r -> d is LinkedList && r is LinkedList },
            { d, r -> d == null && r == null },
            { d, r ->
                d !is java.util.ArrayDeque<*> && d !is LinkedList && d != null && r !is java.util.ArrayDeque<*> && r !is LinkedList && r != null
            },
            coverage = DoNotCalculate
        )
    }
}