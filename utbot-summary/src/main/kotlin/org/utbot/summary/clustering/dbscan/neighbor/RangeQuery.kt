package org.utbot.summary.clustering.dbscan.neighbor

interface RangeQuery<K> {
    fun findNeighbors(queryKey: K, radius: Float): List<Neighbor<K>>
}