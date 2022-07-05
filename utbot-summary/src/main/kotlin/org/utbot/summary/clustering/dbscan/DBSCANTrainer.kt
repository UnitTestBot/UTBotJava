package org.utbot.summary.clustering.dbscan

class DBSCANTrainer<T>(val eps: Float, val minSamples: Int, val metric: Distance<T>) {
    init {
        require(minSamples > 0) { "MinSamples parameter should be more than 0: $minSamples" }
        require(eps > 0.0f) { "Eps parameter should be more than 0: $eps" }
    }


    fun fit(data: Array<T>): DBSCANModel {
        val numberOfClusters = 0
        val clusterLabels = IntArray(data.size)
        val clusterSizes = IntArray(numberOfClusters)
        return DBSCANModel(k = numberOfClusters, clusterLabels = clusterLabels, clusterSizes = clusterSizes)
    }
}