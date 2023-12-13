package org.utbot.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtResult

object UsvmSymbolicEngine {
    // TODO implement
    fun runUsvmGeneration(
        methods: List<ExecutableId>,
        classpath: String,
        timeoutMillis: Long
    ): Flow<Pair<ExecutableId, UtResult>> =
        emptyFlow()
}