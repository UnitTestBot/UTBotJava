package org.utbot.summary.clustering.dbscan.neighbor

/** This is a basic interface for our approaches to ask the set of all points return the subset of the closest neighbors. */
interface RangeQuery<K> {
    /** Returns the list of the closest neighbors in the [radius] from the [queryKey]. */
    fun findNeighbors(queryKey: K, radius: Float): List<Neighbor<K>>
}