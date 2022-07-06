package org.utbot.summary.clustering.dbscan

import org.utbot.summary.clustering.dbscan.neighbor.RangeQuery

/**
 * @property k
 * @property clusterLabels
 * @property clusterSizes  The number of observations in each cluster.
 */
class DBSCANModel<K>(
    val k: Int = 0,
    val clusterLabels: IntArray,
    val clusterSizes: IntArray,
    val rangeQuery: RangeQuery<K>,
    val eps: Float,
    val minSamples: Int
) {
    /** Find a cluster for new data. */
   /* fun predictCluster(data: K): Int {
        val neighbors = rangeQuery.findNeighbors(data, eps)

        if(neighbors.size < minSamples) {
            return NOISE
        }


    }*/
}