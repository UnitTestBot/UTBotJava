package org.utbot.contest.usvm

import org.utbot.framework.plugin.api.mapper.UtModelMapper
import org.utbot.framework.plugin.api.mapper.mapModelIfExists
import org.utbot.framework.plugin.api.mapper.mapModels

fun UtUsvmExecution.mapModels(mapper: UtModelMapper) = copy(
    stateBefore = stateBefore.mapModels(mapper),
    stateAfter = stateAfter.mapModels(mapper),
    result = result.mapModelIfExists(mapper),
    coverage = this.coverage,
    summary = this.summary,
    testMethodName = this.testMethodName,
    displayName = this.displayName,
    instrumentation = instrumentation.map { it.mapModels(mapper) },
)