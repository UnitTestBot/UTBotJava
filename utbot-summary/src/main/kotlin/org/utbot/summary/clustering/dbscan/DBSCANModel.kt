package org.utbot.summary.clustering.dbscan

import org.utbot.summary.clustering.dbscan.neighbor.RangeQuery

/**
 * Keeps the information about clusters produced by [DBSCANTrainer].
 *
 * @property [k] Number of clusters.
 * @property [clusterLabels] Labels of clusters in the range [0; k).
 */
data class DBSCANModel(
    val k: Int = 0,
    val clusterLabels: IntArray
)