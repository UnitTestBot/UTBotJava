package org.utbot.examples.algorithms

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.eq
import org.utbot.examples.ignoreExecutionsNumber
import org.utbot.examples.isException
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

internal class GraphTest : UtValueTestCaseChecker(testClass = GraphExample::class) {
    @Test
    @Tag("slow")
    fun testRunFindCycle() {
        check(
            GraphExample::runFindCycle,
            eq(1),
        )
    }

    @Test
    fun testDijkstra() {
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