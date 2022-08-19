package org.utbot.summary.tag

import org.utbot.framework.plugin.api.UtSymbolicExecution
import org.utbot.summary.SummarySentenceConstants.NEW_LINE
import org.utbot.summary.clustering.SplitSteps


class TraceTag(val execution: UtSymbolicExecution, splitSteps: SplitSteps) : TraceTagWithoutExecution(execution, splitSteps) {
    override fun toString(): String {
        return "${NEW_LINE}Input params:${execution.stateBefore.parameters} Output: $result${NEW_LINE} ${rootStatementTag.toString()}"
    }

    fun fullPrint(): String {
        return "${NEW_LINE}Input params:${execution.stateBefore.parameters} Output: $result${NEW_LINE}" +
                "Method: ${rootStatementTag?.fullPrint()}"
    }

    // sometimes it is easier just to look at the executions steps with our analysis
    fun executionStepsStructure(): String = "${rootStatementTag?.executionStepsStructure()}"
}
