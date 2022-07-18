package org.utbot.summary.clustering.dbscan.neighbor

/**
 * Neighbor abstraction for algorithms with searching in metric space specialization.
 *
 * @property [key] Search key.
 * @property [index] Direct index to access the point in the basic data structure that keeps a set of points.
 * @property [distance] Numerical value that keeps distance from the [key] point in the chosen metric space.
 *
 * NOTE: Neighbors should be ordered and this is implemented via [Comparable] interface.
 */
class Neighbor<K>(val key: K, val index: Int, private val distance: Double) : Comparable<Neighbor<K>> {
    override fun compareTo(other: Neighbor<K>): Int {
        val distance = distance.compareTo(other.distance)
        return if (distance == 0) index.compareTo(other.index) else distance
    }
}