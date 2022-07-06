package org.utbot.summary.clustering.dbscan


interface Metric<T> {
    fun compute(object1: T, object2: T): Double
}
