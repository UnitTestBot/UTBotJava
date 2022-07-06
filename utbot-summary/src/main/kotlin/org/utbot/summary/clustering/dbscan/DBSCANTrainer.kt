package org.utbot.summary.clustering.dbscan

import org.utbot.summary.clustering.dbscan.neighbor.LinearRangeQuery
import org.utbot.summary.clustering.dbscan.neighbor.RangeQuery

private const val NOISE = -3
private const val CLUSTER_PART = -2
private const val UNDEFINED = -1

class DBSCANTrainer<T>(val eps: Float, val minSamples: Int, val metric: Metric<T>, val rangeQuery: RangeQuery<T>) {
    init {
        require(minSamples > 0) { "MinSamples parameter should be more than 0: $minSamples" }
        require(eps > 0.0f) { "Eps parameter should be more than 0: $eps" }
    }


    fun fit(data: Array<T>): DBSCANModel {
        if (rangeQuery is LinearRangeQuery) {
            rangeQuery.data = data
            rangeQuery.metric = metric
        } // TODO: could be refactored if we add some new variants of RangeQuery

        val numberOfClusters = 0
        val clusterLabels = IntArray(data.size)
        val clusterSizes = IntArray(numberOfClusters)

        var clusterCounter = 0
        for (i in data.indices) {
            if(clusterLabels[i] == UNDEFINED) {
                val neigbors = rangeQuery.findNeighbors(data[i], eps)
                if (neigbors.size < minSamples) {


            }
        }


                if |N| < minPts then {                              /* Density check */
                label(P) := Noise                               /* Label as Noise */
                continue
            }
                C := C + 1                                          /* next cluster label */
                label(P) := C                                       /* Label initial point */
                SeedSet S := N \ {P}                                /* Neighbors to expand */
                for each point Q in S {                             /* Process every seed point Q */
                    if label(Q) = Noise then label(Q) := C          /* Change Noise to border point */
                    if label(Q) ≠ undefined then continue           /* Previously processed (e.g., border point) */
                    label(Q) := C                                   /* Label neighbor */
                    Neighbors N := RangeQuery(DB, distFunc, Q, eps) /* Find neighbors */
                    if |N| ≥ minPts then {                          /* Density check (if Q is a core point) */
                        S := S ∪ N                                  /* Add new neighbors to seed set */
                    }
                }
            }
        }


        return DBSCANModel(k = numberOfClusters, clusterLabels = clusterLabels, clusterSizes = clusterSizes)
    }

    private enum class PointStatus { }
}