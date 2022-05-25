package org.utbot.analytics

import org.utbot.engine.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.selectors.strategies.TraverseGraphStatistics
import org.utbot.engine.stmts
import org.utbot.framework.UtSettings
import java.io.File
import java.io.FileOutputStream
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}


class CoverageStatistics(
    private val method: String,
    private val globalGraph: InterProceduralUnitGraph
): TraverseGraphStatistics(globalGraph) {

    private val outputFile: String = "${UtSettings.coverageStatisticsDir}/$method.txt"

    init {
        File(outputFile).printWriter().use { out ->
            out.println("TIME,COV_TARGET_STMT,TOTAL_TARGET_STMT,COV_ALL_STMT,TOTAL_ALL_STMT")
            out.println("${System.nanoTime()}" + "," + getStatistics())
        }
    }

    override fun onTraversed(executionState: ExecutionState) {
        runCatching {
            FileOutputStream(outputFile, true).bufferedWriter()
                .use { out ->
                    out.write(System.nanoTime().toString() + "," + getStatistics())
                    out.newLine()
                }
        }.onFailure {
            logger.warn { "Failed to save statistics: ${it.message}" }
        }
    }

    fun getStatistics() = with(globalGraph) {
        val allStmts = this.graphs.flatMap { it.stmts }
        val graphStmts = this.graphs.first().stmts

        "${graphStmts.filter { this.isCovered(it) }.size},${graphStmts.size}," +
                "${allStmts.filter { this.isCovered(it) }.size},${allStmts.size}"
    }
}