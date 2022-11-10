package org.utbot.engine.taint.timeout

import org.utbot.engine.taint.TaintCandidates
import org.utbot.framework.plugin.api.ExecutableId

sealed interface TaintTimeoutStrategy {
    fun splitTimeout(
        totalTimeoutMs: Long,
        taintCandidates: TaintCandidates
    ): Map<ExecutableId, Long>
}

object SimpleTaintTimeoutStrategy : TaintTimeoutStrategy {
    override fun splitTimeout(totalTimeoutMs: Long, taintCandidates: TaintCandidates): Map<ExecutableId, Long> {
        if (taintCandidates.isEmpty()) {
            return emptyMap()
        }

        // TODO process empty candidates
        val sameTimeout = totalTimeoutMs / taintCandidates.size

        val lastTimeout = totalTimeoutMs - (taintCandidates.size - 1) * sameTimeout

        return taintCandidates.keys.mapIndexed { index, executableId ->
            if (index != taintCandidates.size - 1) {
                executableId to sameTimeout
            } else {
                executableId to lastTimeout
            }
        }.toMap()
    }
}
