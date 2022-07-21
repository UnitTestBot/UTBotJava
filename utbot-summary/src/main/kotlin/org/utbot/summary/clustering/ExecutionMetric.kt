package org.utbot.summary.clustering

import org.utbot.framework.plugin.api.Step
import org.utbot.summary.clustering.dbscan.Metric

/** The existing implementation of [Metric] for the space of [Step]. */
class ExecutionMetric : Metric<Iterable<Step>> {
    /**
     * Minimum Edit Distance
     */
    private fun compareTwoPaths(path1: Iterable<Step>, path2: Iterable<Step>): Double {
        val distances = Array(path1.count()) { i -> Array(path2.count()) { j -> i + j } }

        for (i in 1 until path1.count()) {
            for (j in 1 until path2.count()) {
                val stmt1 = path1.elementAt(i)
                val stmt2 = path2.elementAt(j)

                val d1 = distances[i - 1][j] + 1 // path 1 insert ->  diff stmt from path2
                val d2 = distances[i][j - 1] + 1 // path 2 insert -> diff stmt from path1
                val d3 = distances[i - 1][j - 1] + distance(stmt1, stmt2) // aligned or diff
                distances[i][j] = minOf(d1, d2, d3)
            }
        }
        return distances.last().last().toDouble()
    }

    private fun distance(stmt1: Step, stmt2: Step): Int {
        return if (stmt1 == stmt2) 0 else 2
    }

    override fun compute(object1: Iterable<Step>, object2: Iterable<Step>): Double {
        return compareTwoPaths(object1, object2)
    }
}