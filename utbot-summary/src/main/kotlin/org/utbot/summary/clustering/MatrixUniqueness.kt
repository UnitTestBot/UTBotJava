package org.utbot.summary.clustering

import org.utbot.framework.plugin.api.Step
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.summary.UtSummarySettings
import smile.clustering.dbscan

class MatrixUniqueness(executions: List<UtExecution>) {

    private var methodExecutions: List<UtExecution> = executions
    private val allSteps = mutableListOf<Step>()
    private val matrix: List<IntArray>

    init {
        executions.forEach {
            it.path.forEach { step ->
                if (step !in allSteps) allSteps.add(step)
            }
        }
        this.matrix = this.createMatrix()
    }

    /**
     * Creates uniquness matrix. Rows are executions, columns are unique steps from all executions
     * Every matrix i,j is 1 or 0, as if step in execution or not.
     */
    private fun createMatrix(): List<IntArray> {
        val matrix = mutableListOf<IntArray>()
        for (stmtSeq in this.methodExecutions) {
            val matrixLine = IntArray(allSteps.size)
            for (step in stmtSeq.path) {
                val matrixIndex = allSteps.indexOf(step)
                if (matrixIndex >= 0) matrixLine[matrixIndex] += 1 //+ index
            }
            matrix.add(matrixLine)
        }
        return matrix
    }

    /**
     * @return sum of column number col in matrix
     */
    private fun colSum(matrix: List<IntArray>, col: Int) =
        matrix.sumBy { row -> if (row[col] >= 1) 1 else 0 } //adds 1 if positive else 0

    /**
     * @return vector of column sums
     */
    private fun colSums(matrix: List<IntArray>) = matrix.first().indices.map { col -> this.colSum(matrix, col) }

    /**
     * Splits all steps into common, partly common and unique
     *
     * Unique steps are steps that only occur in one execution
     * Common steps are steps that occur in all executions
     * Partly common steps are steps that occur more than one time, but not in all executions
     */
    fun splitSteps(): SplitSteps {
        if (matrix.size == 1) return SplitSteps(listOf(), listOf(), allSteps.toList())

        val commonSteps = mutableListOf<Step>()
        val partlyCommonSteps = mutableListOf<Step>()
        val uniqueSteps = mutableListOf<Step>()

        val sums = colSums(matrix)
        for (index in sums.indices) {
            when {
                sums[index] >= matrix.size -> commonSteps.add(allSteps[index])
                sums[index] > 1 -> partlyCommonSteps.add(allSteps[index])
                else -> uniqueSteps.add(allSteps[index])
            }
        }
        return SplitSteps(commonSteps, partlyCommonSteps, uniqueSteps)
    }

    companion object {
        /**
         * Returns map: cluster identifier, List<executions>
         *     DBSCAN - Density-Based Spatial Clustering of Applications with Noise
         *     Finds core samples of high density and expands clusters from them
         */
        fun dbscanClusterExecutions(
            methodExecutions: List<UtExecution>,
            minPts: Int = UtSummarySettings.MIN_EXEC_DBSCAN,
            radius: Double = UtSummarySettings.RADIUS_DBSCAN
        ): Map<Int, List<UtExecution>> {
            val executionPaths = methodExecutions.map { it.path.asIterable() }.toTypedArray()
            val cluster = dbscan(executionPaths, ExecutionDistance(), minPts, radius)
            return methodExecutions.withIndex().groupBy({ cluster.y[it.index] }, { it.value })
        }
    }
}

/**
 * Structure that contains common, partly common and unique executions
 */
data class SplitSteps(
    val commonSteps: List<Step> = listOf(),
    val partlyCommonSteps: List<Step> = listOf(),
    val uniqueSteps: List<Step> = listOf()
)