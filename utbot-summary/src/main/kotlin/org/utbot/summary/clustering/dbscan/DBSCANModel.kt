package org.utbot.summary.clustering.dbscan

/**
 * Keeps the information about clusters produced by [DBSCANTrainer].
 *
 * @property [numberOfClusters] Number of clusters.
 * @property [clusterLabels] It contains labels of clusters in the range ```[0; k)```
 * or [Int.MIN_VALUE] if point could not be assigned to any cluster.
 */
data class DBSCANModel(
    val numberOfClusters: Int = 0,
    val clusterLabels: IntArray
)