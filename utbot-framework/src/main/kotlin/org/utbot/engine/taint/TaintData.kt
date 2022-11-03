package org.utbot.engine.taint

import org.utbot.framework.plugin.api.ExecutableId
import soot.jimple.Stmt

data class TaintSourceData(
    val stmt: Stmt,
    val outerMethod: ExecutableId,
    val taintKinds: TaintKinds
)

data class TaintSinkData(
    val stmt: Stmt,
    val outerMethod: ExecutableId,
    val taintIndex: Int
)
