package org.utbot.summary.clustering.dbscan

import org.utbot.summary.clustering.dbscan.neighbor.LinearRangeQuery
import org.utbot.summary.clustering.dbscan.neighbor.RangeQuery

const val NOISE = -3
const val CLUSTER_PART = -2
const val UNDEFINED = -1

class DBSCANTrainer<T>(val eps: Float, val minSamples: Int, val metric: Metric<T>, val rangeQuery: RangeQuery<T>) {
    init {
        require(minSamples > 0) { "MinSamples parameter should be more than 0: $minSamples" }
        require(eps > 0.0f) { "Eps parameter should be more than 0: $eps" }
    }

    fun fit(data: Array<T>): DBSCANModel<T> {
        if (rangeQuery is LinearRangeQuery) {
            rangeQuery.data = data
            rangeQuery.metric = metric
        } // TODO: could be refactored if we add some new variants of RangeQuery

        val numberOfClusters = 0
        val labels = IntArray(data.size) { _ -> UNDEFINED }
        val clusterSizes = IntArray(numberOfClusters)

        var k = 0 // cluster index
        for (i in data.indices) {
            if(labels[i] == UNDEFINED) {
                val neigbors = rangeQuery.findNeighbors(data[i], eps).toMutableList()
                if (neigbors.size < minSamples) {
                    labels[i] = NOISE
                } else {
                    k++
                    labels[i] = k
                    // expand cluster
                    neigbors.forEach {  // Neighbors to expand
                        if(labels[it.index] == UNDEFINED) {
                            labels[it.index] = CLUSTER_PART // all neighbors of a cluster point became cluster points
                        }
                    }

                    for (j in neigbors.indices) {    // Process every seed point Q
                        val q = neigbors[j]
                        val idx = q.index


                        if (labels[idx] == NOISE) { // Change Noise to border point
                            labels[idx] = k
                        }

                        if (labels[idx] == UNDEFINED || labels[idx] == CLUSTER_PART) {
                            labels[idx] = k


                            val qNeighbors = rangeQuery.findNeighbors(q.key, eps)

                            if (qNeighbors.size >= minSamples) {  // Density check (if Q is a core point)
                                // merge two cluster parts
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
                    }
                }

            }
        }

        return DBSCANModel(k = numberOfClusters, clusterLabels = labels, clusterSizes = clusterSizes, rangeQuery = rangeQuery, eps = eps, minSamples = minSamples)
    }
}