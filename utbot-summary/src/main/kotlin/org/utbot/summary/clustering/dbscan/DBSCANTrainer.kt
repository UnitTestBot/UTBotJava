package org.utbot.summary.clustering.dbscan

import org.utbot.summary.clustering.dbscan.neighbor.LinearRangeQuery
import org.utbot.summary.clustering.dbscan.neighbor.Neighbor
import org.utbot.summary.clustering.dbscan.neighbor.RangeQuery

private const val NOISE = Int.MIN_VALUE
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
        require(data.isNotEmpty()) { "Nothing to learn, data is empty." }

        if (rangeQuery is LinearRangeQuery) {
            rangeQuery.data = data
            rangeQuery.metric = metric
        } // TODO: could be refactored if we add some new variants of RangeQuery

        val labels = IntArray(data.size) { _ -> UNDEFINED }
        var k = 0

        for (i in data.indices) {
            if (labels[i] == UNDEFINED) {
                val neighbors = rangeQuery.findNeighbors(data[i], eps).toMutableList()
                if (neighbors.size < minSamples) {
                    labels[i] = NOISE
                } else {
                    labels[i] = k
                    expandCluster(neighbors, labels, k)
                    k++
                }
            }
        }

        return DBSCANModel(k = k, clusterLabels = labels)
    }

    private fun expandCluster(
        neighbors: MutableList<Neighbor<T>>,
        labels: IntArray,
        k: Int
    ) {
        neighbors.forEach {  // Neighbors to expand.
            if (labels[it.index] == UNDEFINED) {
                labels[it.index] = CLUSTER_PART // All neighbors of a cluster point became cluster points.
            }
        }

        // NOTE: the size of neighbors could grow from iteration to iteration and the classical for-loop in Kotlin could not be used
        var j = 0
        while (j < neighbors.count()) // Process every seed point Q.
        {
            val q = neighbors[j]
            val idx = q.index

            if (labels[idx] == NOISE) { // Change Noise to border point.
                labels[idx] = k
            }

            if (labels[idx] == UNDEFINED || labels[idx] == CLUSTER_PART) {
                labels[idx] = k

                val qNeighbors = rangeQuery.findNeighbors(q.key, eps)

                if (qNeighbors.size >= minSamples) { // Density check (if Q is a core point).
                    mergeTwoGroupsInCluster(qNeighbors, labels, neighbors)
                }
            }
            j++
        }
    }

    private fun mergeTwoGroupsInCluster(
        qNeighbors: List<Neighbor<T>>,
        labels: IntArray,
        neighbors: MutableList<Neighbor<T>>
    ) {
        for (qNeighbor in qNeighbors) {
            val label = labels[qNeighbor.index]
            if (label == UNDEFINED) {
                labels[qNeighbor.index] = CLUSTER_PART
            }

            if (label == UNDEFINED || label == NOISE) {
                neighbors.add(qNeighbor)
            }
        }
    }
}