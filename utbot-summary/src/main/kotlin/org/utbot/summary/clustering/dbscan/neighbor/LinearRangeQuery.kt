package org.utbot.summary.clustering.dbscan.neighbor

import org.utbot.summary.clustering.dbscan.Metric

/**
 * This approach implements brute-force search with complexity O(n).
 *
 * @property [data] The whole dataset to search in it.
 * @property [metric] Metric.
 */
class LinearRangeQuery<K> : RangeQuery<K> {
    lateinit var data: Array<K>
    lateinit var metric: Metric<K>

    override fun findNeighbors(queryKey: K, radius: Float): List<Neighbor<K>> {
        val neighbors = mutableListOf<Neighbor<K>>()
        data.forEachIndexed { index, point ->
            val distance = metric.compute(queryKey, point)
            if (distance <= radius && queryKey != point) {
                neighbors.add(Neighbor(point, index, distance))
            }
        }

        return neighbors
    }
}