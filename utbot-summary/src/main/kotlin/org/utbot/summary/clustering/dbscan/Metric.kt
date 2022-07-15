package org.utbot.summary.clustering.dbscan

interface Metric<T> {
    /** Computes the distance between [object1] and [object2] according the given metric. */
    fun compute(object1: T, object2: T): Double
}
