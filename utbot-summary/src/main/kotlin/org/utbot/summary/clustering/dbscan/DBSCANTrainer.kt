package org.utbot.summary.clustering.dbscan

import org.utbot.summary.clustering.dbscan.neighbor.LinearRangeQuery
import org.utbot.summary.clustering.dbscan.neighbor.Neighbor
import org.utbot.summary.clustering.dbscan.neighbor.RangeQuery

private const val NOISE = -3
private const val CLUSTER_PART = -2
private const val UNDEFINED = -1

/**
 * DBSCAN algorithm implementation.
 *
 * NOTE: The existing implementation with the [LinearRangeQuery] has a complexity O(n^2) in the worst case.
 *
 * @property [eps] The radius of search. Should be more than 0.0.
 * @property [minSamples] The minimum number of samples to form the cluster. Should be more than 0.
 * @property [metric] Metric to calculate distances.
 * @property [rangeQuery] Gives access to the data in the implemented order.
 *
 * @see <a href="https://www.aaai.org/Papers/KDD/1996/KDD96-037.pdf">
 *     A Density-Based Algorithm for Discovering Clusters in Large Spatial Databases with Noise</a>
 */
class DBSCANTrainer<T>(val eps: Float, val minSamples: Int, val metric: Metric<T>, val rangeQuery: RangeQuery<T>) {
    init {
        require(minSamples > 0) { "MinSamples parameter should be more than 0: $minSamples" }
        require(eps > 0.0f) { "Eps parameter should be more than 0: $eps" }
    }

    /** Builds a clustering model based on the given data. */
    fun fit(data: Array<T>): DBSCANModel {
        if (rangeQuery is LinearRangeQuery) {
            rangeQuery.data = data
            rangeQuery.metric = metric
        } // TODO: could be refactored if we add some new variants of RangeQuery

        val numberOfClusters = 0
        val labels = IntArray(data.size) { _ -> UNDEFINED }

        var k = 0
        for (i in data.indices) {
            if (labels[i] == UNDEFINED) {
                val neigbors = rangeQuery.findNeighbors(data[i], eps).toMutableList()
                if (neigbors.size < minSamples) {
                    labels[i] = NOISE
                } else {
                    k++
                    labels[i] = k
                    expandCluster(neigbors, labels, k)
                }
            }
        }

        return DBSCANModel(k = numberOfClusters, clusterLabels = labels)
    }

    private fun expandCluster(
        neigbors: MutableList<Neighbor<T>>,
        labels: IntArray,
        k: Int
    ) {
        neigbors.forEach {  // Neighbors to expand.
            if (labels[it.index] == UNDEFINED) {
                labels[it.index] = CLUSTER_PART // All neighbors of a cluster point became cluster points.
            }
        }

        for (j in neigbors.indices) { // Process every seed point Q.
            val q = neigbors[j]
            val idx = q.index


            if (labels[idx] == NOISE) { // Change Noise to border point.
                labels[idx] = k
            }

            if (labels[idx] == UNDEFINED || labels[idx] == CLUSTER_PART) {
                labels[idx] = k


                val qNeighbors = rangeQuery.findNeighbors(q.key, eps)

                if (qNeighbors.size >= minSamples) { // Density check (if Q is a core point).
                    mergeTwoGroupsInCluster(qNeighbors, labels, neigbors)
                }
            }
        }
    }

    private fun mergeTwoGroupsInCluster(
        qNeighbors: List<Neighbor<T>>,
        labels: IntArray,
        neigbors: MutableList<Neighbor<T>>
    ) {
        for (qNeighbor in qNeighbors) {
            val label = labels[qNeighbor.index]
            if (label == UNDEFINED) {
                labels[qNeighbor.index] = CLUSTER_PART
            }

            if (label == UNDEFINED || label == NOISE) {
                neigbors.add(qNeighbor)
            }
        }
    }
}