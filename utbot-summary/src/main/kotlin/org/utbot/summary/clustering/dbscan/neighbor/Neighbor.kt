package org.utbot.summary.clustering.dbscan.neighbor

class Neighbor<K>(val key: K, val index: Int, val distance: Double): Comparable<Neighbor<K>> {
    override fun compareTo(other: Neighbor<K>): Int {
        val distance = distance.compareTo(other.distance)
        return if (distance == 0) index.compareTo(other.index) else distance
    }
}