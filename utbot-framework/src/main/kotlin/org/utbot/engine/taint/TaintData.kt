package org.utbot.engine.taint

import org.utbot.framework.plugin.api.ExecutableId
import soot.jimple.Stmt

data class TaintSourceData(
    val sourceMethod: ExecutableId,
    val taintKinds: TaintKinds,
    val stmt: Stmt
)

data class TaintSinkData(
    val methodContainingSink: ExecutableId,
    val sinkMethod: ExecutableId,
    val stmt: Stmt,
    val taintIndex: Int
)
