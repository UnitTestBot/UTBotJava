package org.utbot.examples.algorithms

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.ignoreExecutionsNumber
import org.utbot.tests.infrastructure.isException
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class GraphTest : UtValueTestCaseChecker(testClass = GraphExample::class) {
    @Test
    @Tag("slow")
    fun testRunFindCycle() {
        checkWithException(
            GraphExample::runFindCycle,
            ignoreExecutionsNumber,
            { e, r -> e == null && r.isException<NullPointerException>() },
            { e, r -> e != null && e.contains(null) && r.isException<NullPointerException>() },
            { e, r -> e != null && e.any { it.first < 0 || it.first >= 10 } && r.isException<ArrayIndexOutOfBoundsException>() },
            { e, r -> e != null && e.any { it.second < 0 || it.second >= 10 } && r.isException<ArrayIndexOutOfBoundsException>() },
            { e, r -> e != null && e.all { it != null } && r.isSuccess }
        )
    }

    @Test
    fun testDijkstra() {
        // The graph is fixed, there should be exactly one execution path, so no matchers are necessary
        check(
            GraphExample::runDijkstra,
            eq(1)
        )
    }

    /**
     * TODO: fix Dijkstra algorithm.
     */
    @Test
    fun testRunDijkstraWithParameter() {
        checkWithException(
            GraphExample::runDijkstraWithParameter,
            ignoreExecutionsNumber,
            { g, r -> g == null && r.isException<NullPointerException>() },
            { g, r -> g.isEmpty() && r.isException<IndexOutOfBoundsException>() },
            { g, r -> g.size == 1 && r.getOrNull()?.size == 1 && r.getOrNull()?.first() == 0 },
            { g, r -> g.size > 1 && g[1] == null && r.isException<IndexOutOfBoundsException>() },
            { g, r -> g.isNotEmpty() && g.size != g.first().size && r.isException<IndexOutOfBoundsException>() },
            { g, r ->
                val concreteResult = GraphExample().runDijkstraWithParameter(g)
                g.isNotEmpty() && r.getOrNull().contentEquals(concreteResult)
            }
        )
    }
}